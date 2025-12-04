# ❄️🔥 ThermoSurvival
> **Survive the Elements. Master the Temperature.**

![Java Version](https://img.shields.io/badge/Java-17%2B-orange)
![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20%2B-green)
![License](https://img.shields.io/badge/License-MIT-blue)
![Version](https://img.shields.io/badge/Version-1.0.1-brightgreen)

![ThermoSurvival Logo](logo.png)

---

| | |
|:---:|:---|
| **Native Minecraft Version** | 1.20 |
| **Tested Minecraft Versions** | 1.20.x, 1.21.x |
| **Source Code** | [GitHub Repository](https://github.com/parlamentum/ThermoSurvival) |
| **Languages** | Java |
| **Donation** | [Support the Project](https://ko-fi.com/parlamentum) |

---

**ThermoSurvival** is a lightweight, "Vanilla+" Spigot plugin that introduces a realistic temperature mechanic to Minecraft. Survival becomes more challenging as you must manage your body temperature against the harsh elements of biomes, weather, and time.

---

## 📚 Table of Contents
- [Features](#features)
- [Installation](#installation)
- [Mechanics](#mechanics)
  - [Temperature Factors](#temperature-factors)
  - [Progressive Effects System](#progressive-effects-system)
- [Commands](#commands)
- [Configuration](#configuration)
- [Permissions](#permissions)
- [WorldGuard Integration](#worldguard-integration)
- [Changelog](#changelog)

---

## ✨ Features
- **Realistic Temperature System**: -30°C to 50°C range based on real-world temperatures
- **Progressive Effects**: 8 levels of temperature effects (4 heat, 4 cold) with increasing severity
- **Visual Indicators**: Dynamic BossBar colors and wither vignette at extreme temperatures
- **Biome Dependent**: 60+ biomes including all Minecraft 1.21 biomes
- **Environmental Factors**: Height, Weather (Rain/Snow/Storm), and Time of Day affect your temperature
- **Block Interaction**: Warm up near Campfires and Lava; cool down near Ice
- **Armor Insulation**: Leather armor cools you down; Netherite heats you up
- **Consumables**: Eat Stews to cool down, drink Water to cool down
- **WorldGuard Integration**: Disable temperature in specific regions
- **Commands**: Reload config and toggle UI elements
- **Highly Configurable**: Tweak every value to fit your server's difficulty

---

## 📥 Installation
1. Download `ThermoSurvival.jar`.
2. Place it in your server's `plugins` folder.
3. Restart your server.
4. (Optional) Edit `plugins/ThermoSurvival/config.yml` to customize settings.
5. Run `/thermo reload` to apply changes.

---

## ⚙️ Mechanics

### Temperature Factors
Your temperature is calculated based on several factors:

1.  **Biomes**: Each biome has a realistic temperature.
    *   *Extreme Cold*: Frozen Peaks (-25°C), Ice Spikes (-20°C)
    *   *Hot*: Desert (40°C), Nether (50°C)
    *   *Neutral*: Plains (20°C), Forest (18°C)
2.  **Height**:
    *   Going above Y=80 (Mountains) makes it colder.
    *   Going deep underground (below Y=40) warms you.
3.  **Weather**:
    *   Rain: Cooling (-3°C)
    *   Snow: Significant cooling (-8°C)
    *   Storms: Cooling (-5°C)
4.  **Time of Day**:
    *   Day: Slight warming (+2°C)
    *   Night: Cooling (-5°C)
5.  **Blocks**:
    *   **Heat Sources**: Lava (+8°C), Campfire (+5°C), Fire (+6°C), Torch (+1.5°C)
    *   **Cold Sources**: Powder Snow (-8°C), Blue Ice (-6°C), Packed Ice (-4°C)
    *   *Note*: Different blocks have different ranges
6.  **Armor**:
    *   Leather armor provides cooling (insulation)
    *   Netherite armor increases heat (heat retention)

### Progressive Effects System

#### 🥵 Heat Effects
- **Warm** (28°C+): Hunger
- **Hot** (33°C+): Hunger I, Weakness, 0.5 damage/20s
- **VERY HOT** (38°C+): Hunger II, Weakness I, Slowness, 1.0 damage/10s, Fire at 42°C
- **BURNING** (43°C+): Hunger III, Weakness II, Slowness I, Nausea, 2.0 damage/5s, Fire, Wither vignette

#### 🥶 Cold Effects
- **Cool** (10°C-): Slowness
- **Cold** (5°C-): Slowness I, Mining Fatigue, 0.5 damage/20s
- **VERY COLD** (-5°C-): Slowness II, Mining Fatigue I, Hunger, 1.0 damage/10s
- **FREEZING** (-15°C-): Slowness III, Mining Fatigue II, Hunger I, Weakness I, 2.0 damage/5s, Wither vignette

---

## 🎮 Commands

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/thermo reload` | Reload the configuration | `thermosurvival.admin` |
| `/thermo toggle bossbar` | Toggle BossBar display | `thermosurvival.admin` |
| `/thermo toggle actionbar` | Toggle ActionBar display | `thermosurvival.admin` |

---

## 🛠 Configuration
The `config.yml` file allows you to control almost every aspect of the plugin.

### Key Settings
*   `update-interval`: How often temperature is calculated (default: 20 ticks / 1s)
*   `base-temp`: The starting neutral temperature (default: 20°C)
*   `min-temp` / `max-temp`: Temperature range (default: -30°C to 50°C)
*   `disabled-worlds`: List of worlds where the plugin is inactive

### Example Config Snippets

**Biomes**
```yaml
biomes:
  DESERT: 40
  FROZEN_PEAKS: -25
  PLAINS: 20
```

**Blocks**
```yaml
blocks:
  CAMPFIRE: 
    temp: 5.0
    radius: 4
  POWDER_SNOW: -8.0
```

**Progressive Thresholds**
```yaml
thresholds:
  heat_severe:
    trigger: 38
    effects:
      - HUNGER:2
      - WEAKNESS:1
      - SLOWNESS:0
    damage-interval: 200
    damage-amount: 1.0
    fire-tick-trigger: 42
```

---

## 🔐 Permissions

| Permission | Description | Default |
| :--- | :--- | :--- |
| `thermosurvival.bypass` | Allows a player to ignore all temperature effects | OP |
| `thermosurvival.admin` | Allows access to `/thermo` commands | OP |

---

## 🛡️ WorldGuard Integration

ThermoSurvival supports WorldGuard as a soft dependency. Configure regions where temperature effects are disabled:

```yaml
worldguard:
  enabled: true
  disabled-regions:
    - "spawn_region"
    - "safe_zone"
```

If WorldGuard is not installed, the plugin will work normally without region support.

---

## 📝 Changelog

See [CHANGELOG.md](CHANGELOG.md) for detailed version history.

---

*Created by parlamentum*
