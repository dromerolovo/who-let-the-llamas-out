using Microsoft.AspNetCore.SignalR;

namespace Backend
{
    public class LLamasHub : Hub
    {
        private readonly AppState _appState;
        public LLamasHub(AppState appState) 
        { 
            _appState = appState;
        }
        public async Task Connect()
        {
            var counter = _appState.Users.Count + 1;
            var user = new Data.User { Id = counter };
            _appState.Users.TryAdd(counter, user);
            await Clients.Caller.SendAsync("OnConnected", user.Id);
        }
        public async Task CaptureLlama(int llamaId, int userId)
        {
            var user = _appState.Users[userId];
            if(user != null)
            {
                var llama = _appState.Llamas[llamaId];
                llama.Captured = true;
                user.CapturedLlamasCount++;

                //await Clients.All.SendAsync("OnLlamaCaptured", llamaId, userId);
            }

            await Task.CompletedTask;
        }
    }
}
