async function get_weather(params) {
    var city = params.city || "福州";
    try {
        var resp = await fetch("https://wttr.in/" + encodeURIComponent(city) + "?format=j1");
        var data = await resp.json();
        if (!data.current_condition || !data.current_condition[0]) {
            return { success: false, error: "查询失败" };
        }
        var c = data.current_condition[0];
        var desc = c.weatherDesc && c.weatherDesc[0] ? c.weatherDesc[0].value : "未知";
        return {
            success.nearest_area && data.nearest_area[0] ? data.nearest_area[0].areaName[0].value : city,
            temperature: c.temp_C + "°C",
            feelsLike: c.FeelsLikeC + "°C",
            weather: desc,
            humidity: c.humidity + "%",
            wind: c.windspeedKmph + " km/h",
            windDir: c.winddir16Point,
            visibility: c.visibility + " km",
            cloud: c.cloudcover + "%",
            uvIndex: c.uvIndex,
            pressure: c.pressure + " hPa",
            precip: c.precipMM + " mm"
        };
    } catch (e) {
        return { success: false, error: e.message };
    }
}

async function get_forecast(params) {
    var city = params.city || "福州";
    var days = Math.min(params.days || 3, 3);
    try {
        var resp = await fetch("https://wttr.in/" + encodeURIComponent(city) + "?format=j1");
        var data = await resp.json();
        if (!data.weather) {
            return { success: false, error: "预报失败" };
        }
        var forecast = [];
        for (var i = 0; i < days && i < data.weather.length; i++) {
            var d = data.weather[i];
            forecast.push({
                date: d.date,
                maxTemp: d.maxtempC + "°C",
                minTemp: d.mintempC + "°C",
                avgTemp: d.avgtempC + "°C",
                sunHours: d.sunHour,
                uvIndex: d.uvIndex,
                hourly: d.hourly ? d.hourly.map(function(h) {
                    return {
                        time: h.time,
                        temp: h.tempC + "°C",
                        weather: h.weatherDesc && h.weatherDesc[0] ? h.weatherDesc[0].value : "",
                        humidity: h.humidity + "%",
                        wind: h.windspeedKmph + " km/h"
                    };
                }) : []
            });
        }
        return { success: true, city: city, forecast: forecast };
    } catch (e) {
        return { success: false, error: e.message };
    }
}

exports.get_weather = get_weather;
exports.get_forecast = get_forecast;
