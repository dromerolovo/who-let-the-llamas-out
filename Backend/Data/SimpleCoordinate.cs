
using NetTopologySuite.Geometries;

namespace Backend.Data
{
    public class SimpleCoordinate
    {
        public SimpleCoordinate(double X, double Y) { this.X = X; this.Y = Y; }
        public double X { get; set; }
        public double Y { get; set; }
        public Coordinate ToNTS() => new Coordinate(X, Y);
    }
}
