using System.Collections.Concurrent;

namespace Backend
{
    //@Todo implement redis, don't manage everything in memory 
    public class AppState
    {
        public ConcurrentDictionary<int, Data.User> Users { get; } = new ConcurrentDictionary<int, Data.User>();
        public ConcurrentDictionary<int, Data.Llama> Llamas { get; } = new ConcurrentDictionary<int, Data.Llama>();
    }
}
