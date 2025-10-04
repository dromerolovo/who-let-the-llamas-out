using Backend;

var builder = WebApplication.CreateBuilder(args);
var app = builder.Build();

app.MapHub<LLamasHub>("/llamasHub");

app.MapGet("/", () => "Hello World!");

app.Run();
