# GuppyCosmetics

GuppyCosmetics is a Minecraft plugin that allows players to equip customizable cosmetic items, including hats and backblings. Players can easily equip these items by right-clicking on them.

## Features

- **Custom Hats**: Players can equip hats from a configurable list of items.
- **Backblings**: Players can equip backbling items as a chestplate item, with configurable offsets for positioning.
- **Command Support**: Players with appropriate permissions can use commands to spawn cosmetic items for themselves or other players, and reload configuration files.
- **Messages**: Fully customizable messages for equipping items and reloading configurations.
- **Hex Color Support**: Support for hex colors and Minecraft color codes in item names, lore, and messages.

## To-Do List

- [ ] **Improve Documentation**: Add detailed explanations for how to customize items in the configuration files.
- [ ] **Permissions System**: Integrate a permissions system to restrict certain cosmetics to specific players or ranks, as well as permissions for commands.

## Commands

### `/guppycosmetics <spawn|reload>`

- **spawn <item-id> [player]**: Spawn a cosmetic item for a player. If no player is specified, the item will be given to the sender.
- **reload**: Reload all configuration files.

## Configuration

The plugin provides configuration files for hats, backblings, and messages:

- **hats.yml**: Define hats, including material, name, lore, and custom model data.
- **backbling.yml**: Define backblings, including material, name, lore, custom model data, and position offsets.
- **messages.yml**: Customize plugin messages, including color and formatting options.

## Installation

1. Download the `GuppyCosmetics` plugin.
2. Place it in the `plugins` folder of your Minecraft server.
3. Restart or reload your server to generate the necessary configuration files.
4. Edit the configuration files (`hats.yml`, `backbling.yml`, `messages.yml`) to suit your needs.
5. Use the `/guppycosmetics` commands to manage cosmetics.

## License

This project is licensed under the **Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International License (CC BY-NC-ND 4.0)**.

You are free to:

- View and fork this repository for personal use.

Under the following conditions:

- You may not modify, distribute, or take any part of this code for use in other projects.
- You may not use the code for commercial purposes.
- You may not distribute this plugin in any form.

For full terms and conditions, see the full license at:  
[Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International License](https://creativecommons.org/licenses/by-nc-nd/4.0/)

