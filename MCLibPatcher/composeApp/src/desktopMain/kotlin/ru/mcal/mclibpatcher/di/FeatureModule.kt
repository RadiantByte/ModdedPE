package ru.mcal.mclibpatcher.di

import org.koin.core.module.Module

interface FeatureModule {
    val modules: List<Module>
}
