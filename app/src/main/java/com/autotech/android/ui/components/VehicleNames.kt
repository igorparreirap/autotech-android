package com.autotech.android.ui.components

import com.autotech.android.data.local.entity.VehicleEntity
import com.autotech.android.domain.FuelType
import com.autotech.android.domain.VehicleNaming

/* Atalhos de exibição do veículo (regras em domain/VehicleNaming e domain/FuelType). */
val VehicleEntity.originalName: String get() = VehicleNaming.original(brand, model)
val VehicleEntity.displayName: String get() = VehicleNaming.display(nickname, brand, model)
val VehicleEntity.shortName: String get() = VehicleNaming.short(nickname, model)
val VehicleEntity.fuelType: FuelType? get() = FuelType.from(fuel)
/** "Flex", "Diesel"...; null quando o combustível ainda não foi informado. */
val VehicleEntity.fuelLabel: String? get() = fuelType?.label ?: fuel?.takeIf { it.isNotBlank() }
