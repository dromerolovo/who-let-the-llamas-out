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
        public bool Captured { get; set; } = false;

        public LlamaDto ToDto()
        {
            return new LlamaDto
            {
                Id = this.Id,
                Bearing = this.Bearing,
                CurrentPosition = this.CurrentPosition,
                MovementPerSecond = this.MovementPerSecond
            };
        }
    }
}
