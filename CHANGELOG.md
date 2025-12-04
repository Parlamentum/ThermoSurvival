# Changelog

## [1.0.1] - Progressive Temperature System - 2025-12-04

### Added
- **Progressive Temperature Effects System**: 8 levels of temperature effects (4 heat, 4 cold)
  - Heat: Warm (28°C) → Hot (33°C) → Very Hot (38°C) → Burning (43°C)
  - Cold: Cool (10°C) → Cold (5°C) → Very Cold (-5°C) → Freezing (-15°C)
- **Visual Indicators**: 
  - Dynamic BossBar colors (Blue/White/Green/Yellow/Red)
  - Wither vignette effect at extreme temperatures
  - Temperature displayed in Celsius on BossBar
- **WorldGuard Integration**: 
  - Soft dependency support
  - Configurable region exclusions
  - Graceful fallback when WorldGuard not present
- **Comprehensive Biome Support**: 60+ biomes including all Minecraft 1.21 biomes
  - All ocean variants (warm, cold, frozen, deep)
  - All nether biomes
  - All end biomes
  - New 1.21 biomes (Cherry Grove, Deep Dark, Mangrove Swamp, etc.)
- **Commands**:
  - `/thermo reload` - Reload configuration
  - `/thermo toggle bossbar` - Toggle BossBar display
  - `/thermo toggle actionbar` - Toggle ActionBar display
- **Additional Blocks**: Soul Campfire, Soul Torch, Soul Fire support

### Changed
- **Realistic Temperature Range**: Changed from -100/100 to -30°C/50°C (realistic real-world values)
- **Base Temperature**: Changed from 0°C to 20°C (comfortable room temperature)
- **Default Thresholds**: Cold trigger at -5°C, Heat trigger at 33°C (was -50°C and 50°C)
- **UI Defaults**: BossBar enabled by default, ActionBar disabled by default
- **Biome Temperatures**: Adjusted all biomes to realistic Celsius values
  - Desert: 40°C (was 35°C)
  - Nether: 50°C (was 80-85°C)
  - Frozen Peaks: -25°C (was -30°C)
- **Armor Effects**: 
  - Leather now cools you down (insulation)
  - Netherite now heats you up (heat retention)
- **Consumables**: Now cool you down instead of heating up
- **Weather/Time Modifiers**: Adjusted to more realistic values

### Fixed
- **Plugman Compatibility**: Properly cancel tasks on disable to prevent reload errors
- **Null Safety**: Added null checks for all configuration sections
- **Progressive Effects**: Only the most severe applicable effect level is applied (no stacking)

### Technical
- Added WorldGuard Maven dependency
- Created `WorldGuardHook` class for clean integration
- Rewrote `applyEffects()` method for progressive threshold system
- Updated `updateBossBar()` for dynamic status display

## [1.0.0] - 2025-12-03

### Initial Release
- Basic temperature system based on biome, height, weather, time, armor, and nearby blocks
- Simple hot/cold threshold effects
- BossBar and ActionBar display options
- Configurable damage and potion effects
