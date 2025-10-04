using Backend.Data;
using System;
using System.Net.Http;
using System.Text.Json;

namespace Backend
{

    public interface IMapboxDirectionService
    {
        public Task<List<SimpleCoordinate>> GetWalkingRoute(SimpleCoordinate start, SimpleCoordinate end);
    }

    public class MockMapboxDirectionService : IMapboxDirectionService
    {
        private readonly Random _random = new Random();

        public MockMapboxDirectionService(HttpClient httpClient)
        {

        }

        public Task<List<SimpleCoordinate>> GetWalkingRoute(SimpleCoordinate start, SimpleCoordinate end)
        {
            int intermediateCount = _random.Next(6, 15);
            var route = new List<SimpleCoordinate> { start };

            double lastX = start.X;
            double lastY = start.Y;

            for (int i = 1; i <= intermediateCount; i++)
            {
                double t = (double)i / (intermediateCount + 1);
                double x = start.X + (end.X - start.X) * t + (_random.NextDouble() - 0.5) * 0.0005;
                double y = start.Y + (end.Y - start.Y) * t + (_random.NextDouble() - 0.5) * 0.0005;

                route.Add(new SimpleCoordinate(x, y));
                lastX = x;
                lastY = y;
            }

            route.Add(end);
            return Task.FromResult(route);
        }
    }

    public class MapboxDirectionService : IMapboxDirectionService
    {
        private readonly String _mapboxToken;
        private readonly HttpClient _httpClient;

        public MapboxDirectionService(IConfiguration configuration, HttpClient httpClient)
        {
            _mapboxToken = configuration["MAPBOX_API_KEY"] ?? throw new ArgumentNullException("MAPBOX_API_KEY is not configured.");
            _httpClient = httpClient;
        }

        public async Task<List<SimpleCoordinate>> GetWalkingRoute(SimpleCoordinate start, SimpleCoordinate end)
        {
            string url = $"directions/v5/mapbox/walking/{start.X},{start.Y};{end.X},{end.Y}" +
                         $"?geometries=geojson&access_token={_mapboxToken}";

            var response = await _httpClient.GetStringAsync(url);

            var json = JsonDocument.Parse(response);

            var coordinates = json.RootElement
                .GetProperty("routes")[0]
                .GetProperty("geometry")
                .GetProperty("coordinates");

            var coordinatesList = new List<SimpleCoordinate>();

            foreach (var coord in coordinates.EnumerateArray())
            {
                double lon = coord[0].GetDouble();
                double lat = coord[1].GetDouble();
                var coordinate = new SimpleCoordinate(lon, lat);
                coordinatesList.Add(coordinate);
            }


            return coordinatesList;
        }
    }
}
