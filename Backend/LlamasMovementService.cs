
using Backend.Data;
using Microsoft.AspNetCore.SignalR;
using Microsoft.Extensions.Options;
using NetTopologySuite.Geometries;
using NetTopologySuite.IO;
using ProjNet.CoordinateSystems;
using ProjNet.CoordinateSystems.Transformations;
using System.Collections.Concurrent;
using System.Text.Json;

namespace Backend
{
    public class LlamasMovementService : BackgroundService
    {
        private readonly LlamasOptions _llamasOptions;
        private readonly ConcurrentDictionary<int, Llama> _llamas = new ConcurrentDictionary<int, Llama>();
        private readonly IMapboxDirectionService _mapboxDirectionService;
        private readonly IHubContext<LLamasHub> _hubContext;
        public LlamasMovementService
        (
            IOptions<LlamasOptions> llamasOptions,
            IMapboxDirectionService mapboxDirectionService,
            IHubContext<LLamasHub> hubContext

        )
        {
            _mapboxDirectionService = mapboxDirectionService;
            _llamasOptions = llamasOptions.Value;
            _hubContext = hubContext;
        }
        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            while(!stoppingToken.IsCancellationRequested)
            {
                InitializeObjects();
                await UpdateLlamasPositions();
                await BroadcastLlamasPosition();
                await Task.Delay(_llamasOptions.UpdateIntervalMs, stoppingToken);
            }

        }

        private void InitializeObjects()
        {
            for (var i = 0; i < _llamasOptions.ObjectsCount; i++)
            {
                var @object = new Llama
                {
                    Id = i,
                };
                _llamas.TryAdd(i, @object);
            }
        }

        private async Task BroadcastLlamasPosition()
        {
            var dtos = _llamas.Values.Select(v => new LlamaDto
            {
                Id = v.Id,
                CurrentPosition = v.CurrentPosition,
                Bearing = v.Bearing,
                MovementPerSecond = v.MovementPerSecond
            }).ToList();

            await _hubContext.Clients.All.SendAsync("LlamasPositions", _llamas.Values.ToList());

            Console.WriteLine(JsonSerializer.Serialize(dtos));
        }

        private async Task UpdateLlamasPositions()
        {
            var llamasNeedingUpdate = _llamas.Values.Where(llm => llm.NeedsNewRoute).ToList();

            if(llamasNeedingUpdate.Any())
            {
                var routeTasks = llamasNeedingUpdate.Select(async llm =>
                {
                    var pointOrigin = llm.PreviousPosition ?? GetRandomPoint();
                    var pointDestination = GetRandomPoint();
                    var newRoute = await _mapboxDirectionService.GetWalkingRoute(pointOrigin, pointDestination);

                    llm.CurrentPosition = pointOrigin;
                    llm.CurrentRoute = newRoute;
                    llm.NeedsNewRoute = false;
                    llm.RouteDestination = newRoute.Last();
                    llm.CurrentRouteIndex = 0;
                });

                await Task.WhenAll(routeTasks);
            }

            foreach (var llm in _llamas.Values)
            {
                if (llm.CurrentRoute == null)
                {
                    llm.NeedsNewRoute = true;
                    continue;
                }

                llm.PreviousPosition = llm.CurrentPosition;

                var target = llm.CurrentRoute[llm.CurrentRouteIndex];
                var distanceMeters = CalculateDistance(llm.CurrentPosition, target);

                if (distanceMeters <= _llamasOptions.ObjectsSpeed)
                {
                    llm.CurrentPosition = target;
                    llm.CurrentRouteIndex++;


                    if (llm.CurrentRouteIndex >= llm.CurrentRoute.Count)
                    {
                        llm.NeedsNewRoute = true;
                    }
                }
                else
                {
                    llm.CurrentPosition = MoveTowardsInMeters(llm.CurrentPosition, target);
                }

                llm.Bearing = CalculateBearing(llm.PreviousPosition, llm.CurrentPosition);
                llm.MovementPerSecond = CalculateMovementPerSecond(llm.PreviousPosition, llm.CurrentPosition);
            }

        }

        private SimpleCoordinate MoveTowardsInMeters(SimpleCoordinate currentPosition, SimpleCoordinate target)
        {
            var distance = CalculateDistance(currentPosition, target);
            if (distance <= 0) return currentPosition;

            var ratio = _llamasOptions.ObjectsSpeed / distance;

            var newX = currentPosition.X + (target.X - currentPosition.X) * ratio;
            var newY = currentPosition.Y + (target.Y - currentPosition.Y) * ratio;

            return new SimpleCoordinate(newX, newY);
        }

        private SimpleCoordinate GetRandomPoint()
        {
            var polygon = GetBoundaries();
            var random = new Random();
            var envelope = polygon.EnvelopeInternal;
            while (true)
            {
                double randomX = envelope.MinX + random.NextDouble() * (envelope.MaxX - envelope.MinX);
                double randomY = envelope.MinY + random.NextDouble() * (envelope.MaxY - envelope.MinY);
                var point = new Point(randomX, randomY);
                if (polygon.Contains(point))
                {
                    return new SimpleCoordinate(randomX, randomY);
                }
            }
        }

        private Polygon GetBoundaries()
        {
            WKTReader wktReader = new WKTReader();

            var wkt = _llamasOptions.Boundary;

            Geometry geometry = wktReader.Read(wkt);

            if (geometry is Polygon polygon)
            {
                return polygon;
            }
            else
            {
                throw new Exception("Boundary WKT is not a valid Polygon.");
            }
        }

        private double CalculateDistance(SimpleCoordinate origin, SimpleCoordinate destination)
        {
            var gf = new GeometryFactory(new PrecisionModel(), 4326);
            var point1 = gf.CreatePoint(origin.ToNTS());
            var point2 = gf.CreatePoint(destination.ToNTS());

            var wgs84 = GeographicCoordinateSystem.WGS84;
            var webMercator = ProjectedCoordinateSystem.WebMercator;

            var ctFactory = new CoordinateTransformationFactory();
            var transform = ctFactory.CreateFromCoordinateSystems(wgs84, webMercator);

            var mercatorOrigin = transform.MathTransform.Transform(new double[] { point1.X, point1.Y });
            var mercatorDestination = transform.MathTransform.Transform(new double[] { point2.X, point2.Y });


            var webMercatorFactory = new GeometryFactory(new PrecisionModel(), 3857);
            var point1Meters = webMercatorFactory.CreatePoint(new Coordinate(mercatorOrigin[0], mercatorOrigin[1]));
            var point2Meters = webMercatorFactory.CreatePoint(new Coordinate(mercatorDestination[0], mercatorDestination[1]));

            double distanceMeters = point1Meters.Distance(point2Meters);
            return distanceMeters;
        }

        private double CalculateBearing(SimpleCoordinate from, SimpleCoordinate to)
        {
            var lat1 = DegreesToRadians(from.Y);
            var lat2 = DegreesToRadians(to.Y);
            var dLon = DegreesToRadians(to.X - from.X);

            var y = Math.Sin(dLon) * Math.Cos(lat2);
            var x = Math.Cos(lat1) * Math.Sin(lat2) -
                    Math.Sin(lat1) * Math.Cos(lat2) * Math.Cos(dLon);

            var bearingRadians = Math.Atan2(y, x);

            var bearingDegrees = RadiansToDegrees(bearingRadians);

            return (bearingDegrees + 360) % 360;
        }

        private SimpleCoordinate CalculateMovementPerSecond(SimpleCoordinate origin, SimpleCoordinate dest)
        {
            return new SimpleCoordinate(dest.X - origin.X, dest.Y - origin.Y);
        }

        private double RadiansToDegrees(double radians)
        {
            return radians * 180.0 / Math.PI;
        }

        private double DegreesToRadians(double degrees)
        {
            return degrees * Math.PI / 180.0;
        }
    }
}
