namespace Backend.Data
{
    public class Llama
    {
        public int Id { get; set; }
        public SimpleCoordinate? CurrentPosition { get; set; }
        public bool NeedsNewRoute { get; set; } = true;
        public SimpleCoordinate? PreviousPosition { get; set; }
        public List<SimpleCoordinate>? CurrentRoute { get; set; }
        public int CurrentRouteIndex { get; set; } = 0;
        public SimpleCoordinate? RouteDestination { get; set; }
        public double Bearing { get; set; }
        public SimpleCoordinate? MovementPerSecond { get; set; }
    }
}
