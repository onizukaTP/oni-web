package com.onizuka.framework.util;

public class Banner {

    public static void print(int port, int workers, int routeCount, long startupTimeMs) {
        System.out.printf("  \u001B[32m:: OniWeb Framework ::\u001B[0m (v1.0.0-MVP)\n\n");
        System.out.printf("  -> Port:            \u001B[33m%d\u001B[0m\n", port);
        System.out.printf("  -> Worker Threads:  \u001B[33m%d\u001B[0m\n", workers);
        System.out.printf("  -> Registered Routes: \u001B[33m%d\u001B[0m\n", routeCount);
        System.out.printf("  -> Ready in:        \u001B[32m%d ms\u001B[0m\n\n", startupTimeMs);
    }
}
