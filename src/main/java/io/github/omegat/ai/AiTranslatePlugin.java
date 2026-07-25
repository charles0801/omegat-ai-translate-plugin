package io.github.omegat.ai;

import org.omegat.core.Core;

/** OmegaT plugin entry point. */
public final class AiTranslatePlugin {
    private AiTranslatePlugin() {
    }

    public static void loadPlugins() {
        Core.registerMachineTranslationClass(AiTranslate.class);
    }

    public static void unloadPlugins() {
        // OmegaT owns the registered translator instance for the process lifetime.
    }
}

