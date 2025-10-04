using Backend;

var builder = WebApplication.CreateBuilder(args);

builder.Configuration.AddUserSecrets<Program>();

var services = builder.Services;

services.AddSignalR();

services.Configure<LlamasOptions>(builder.Configuration.GetSection("LlamasOptions"));

builder.Services.AddHttpClient<IMapboxDirectionService, MockMapboxDirectionService>(client =>
{
    client.BaseAddress = new Uri("https://api.mapbox.com/");
    client.Timeout = TimeSpan.FromSeconds(30);
});


services.AddHostedService<LlamasMovementService>();

var app = builder.Build();

app.MapHub<LLamasHub>("/llamas-hub");

app.Run();
