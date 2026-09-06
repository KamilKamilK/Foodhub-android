package pl.foodhub.pos.core.fiscal.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pl.foodhub.pos.core.fiscal.FiscalManufacturer
import pl.foodhub.pos.core.fiscal.FiscalPrinterDriver
import pl.foodhub.pos.core.fiscal.novitus.NovitusFiscalDriver
import pl.foodhub.pos.core.fiscal.posnet.PosnetFiscalDriver
import javax.inject.Singleton

/**
 * A plain `Map` built from two concrete, `@Inject`-constructed drivers -- this
 * codebase has no existing Dagger multibinding (`@IntoMap`/`@MapKey`) precedent, and
 * two entries doesn't warrant introducing one.
 */
@Module
@InstallIn(SingletonComponent::class)
object FiscalModule {
    @Provides
    @Singleton
    @JvmSuppressWildcards
    fun fiscalDrivers(
        novitus: NovitusFiscalDriver,
        posnet: PosnetFiscalDriver,
    ): Map<FiscalManufacturer, FiscalPrinterDriver> =
        mapOf(
            FiscalManufacturer.NOVITUS to novitus,
            FiscalManufacturer.POSNET_ONLINE to posnet,
        )
}
