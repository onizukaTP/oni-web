package com.onizuka.framework;

import com.onizuka.framework.server.NioHttpServer;
import com.onizuka.framework.server.StaticFileHandler;
import com.onizuka.framework.server.dispatcher.RequestDispatcher;
import com.onizuka.framework.server.middleware.Middleware;
import com.onizuka.framework.server.middleware.implementations.CorsMiddleware;
import com.onizuka.framework.server.middleware.implementations.LoggingMiddleware;
import com.onizuka.framework.server.middleware.implementations.RateLimitMiddleware;
import com.onizuka.framework.server.routing.RouteRegistry;
import com.onizuka.framework.util.Banner;
import com.onizuka.framework.util.ClassScanner;
import com.onizuka.framework.util.OniLogger;

import java.util.ArrayList;
import java.util.List;

public class OniWeb {

    private static final OniLogger log = OniLogger.get(OniWeb.class);

    public static NioHttpServer start(Class<?> primarySourceClass) {
        return start(primarySourceClass, new OniWebConfig());
    }

    public static NioHttpServer start(Class<?> primarySourceClass, int port) {
        return start(primarySourceClass, new OniWebConfig().setPort(port));
    }

    public static NioHttpServer start(Class<?> primarySourceClass, OniWebConfig config) {
        long startTime = System.currentTimeMillis();
        OniLogger.setGlobalLevel(config.getLogLevel());

        RouteRegistry registry = new RouteRegistry();
        List<Class<?>> controllers = ClassScanner.findControllers(primarySourceClass);

        for (Class<?> controller : controllers) {
            registry.registerController(controller);
        }

        List<Middleware> middlewares = new ArrayList<>();
        middlewares.add(new LoggingMiddleware());
        if (config.isEnableCors()) {
            middlewares.add(new CorsMiddleware());
        }
        if (config.isEnableRateLimit()) {
            middlewares.add(new RateLimitMiddleware(config.getMaxRequestsPerMinute()));
        }

        StaticFileHandler staticFileHandler = new StaticFileHandler(config.getStaticDir());
        RequestDispatcher dispatcher = new RequestDispatcher(registry, staticFileHandler);

        NioHttpServer server = new NioHttpServer(config, middlewares, dispatcher);

        Banner.print(config.getPort(), config.getWorkerThreads(), registry.getRoutes().size(), System.currentTimeMillis() - startTime);

        new Thread(server::start, "oni-main-loop").start();

        return server;
    }
}
