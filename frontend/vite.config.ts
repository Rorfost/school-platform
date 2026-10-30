import path from "node:path";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv, type Plugin } from "vite";

function htmlEnvPlugin(env: Record<string, string>): Plugin {
    return {
        name: "html-env-transform",
        transformIndexHtml(html) {
            const logo = env.VITE_SCHOOL_LOGO?.trim() || "/favicon.svg";
            const name = env.VITE_SCHOOL_NAME?.trim();
            const title = name ? `${name} | શાળા પોર્ટલ` : "શાળા પોર્ટલ";
            return html
                .replace("%VITE_SCHOOL_LOGO%", logo)
                .replace("%VITE_SCHOOL_NAME% | શાળા પોર્ટલ", title)
                .replace("%VITE_SCHOOL_NAME%", name || "શાળા પોર્ટલ");
        },
    };
}

export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), "");
    return {
        plugins: [react(), tailwindcss(), htmlEnvPlugin(env)],
        resolve: {
            alias: {
                "@": path.resolve(import.meta.dirname, "./src"),
            },
        },
        test: {
            environment: "jsdom",
            globals: true,
            setupFiles: "./src/test/setup.ts",
        },
    };
});
