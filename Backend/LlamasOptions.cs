namespace Backend
{
    public class LlamasOptions
    {
        public int ObjectsCount { get; set; } = 20;
        public double ObjectsSpeed { get; set; } = 0.5;
        public string Boundary { get; set; } = null!;
        public int UpdateIntervalMs { get; set; } = 1500;
    }
}
