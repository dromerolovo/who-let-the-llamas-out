
using Backend.Data;
using Microsoft.AspNetCore.SignalR;
using Microsoft.Extensions.Options;
using System.Collections.Concurrent;

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
        protected override Task ExecuteAsync(CancellationToken stoppingToken)
        {
            InitializeObjects();
            throw new NotImplementedException();
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

        private async Task UpdateLlamasPositions()
        {
            var llamasNeedingUpdate = _llamas.Values.Where(o => o.NeedsNewRoute).ToList();

        }
    }
}
