namespace Backend.Data
{
    public class LlamaDto
    {
        public int Id { get; set; }
        public double Bearing { get; set; }
        public SimpleCoordinate? CurrentPosition { get; set; }
        public SimpleCoordinate? MovementPerSecond { get; set; }
    }
}
