package com.walcker.games.features.data.platform

import org.koin.core.module.Module

/**
 * Koin module providing platform-specific services for the games product.
 *
 * The actual binding is provided by androidMain / iosMain, where the
 * underlying DataStore file paths can be resolved.
 */
internal expect val gamesPlatformModule: Module
