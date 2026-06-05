# BYOW Island Generator

A procedurally generated 2D island world engine built entirely from scratch in Java with no skeleton code or starter implementation. Every system in this project including world generation, pathfinding, interactivity, and saving and loading was designed and written by myself.

Animal Crossing has been one of my favorite games since childhood. Personally, choosing this theme for the final project of 61B made it much more fun and engaging to create. The idea of a peaceful island full of named facilities, apple trees, and a villager you control felt like the perfect theme to bring this engine to life.

## Features

**World Generation**
Seeded pseudorandom generation produces a unique island for every seed. **BFS** connects all facilities so every floor tile is reachable from any other.

**Villager Movement**
Move your villager (the very adorable Marshall) around the island using WASD. Movement is restricted to walkable floor tiles.

**Click to Path**
Click any reachable tile to preview the **shortest BFS** path highlighted on screen. Click the same tile again to animate the villager walking through it step by step.

**Undo and Redo**
Press U to undo the most recent move and K to redo it. Move history is maintained throughout the session.

**Save and Load**
Press :Q to save your game and quit. Press L from the main menu to restore your session exactly as you left it.

**HUD**
Hover over any tile to see its name displayed in real time at the top of the screen. Each tile represents plant life and facilities found in the Animal Crossing game such as the Museum, Plaza, Airport, fruit trees, flowers, and more!

**Music**
Background music plays on loop from the main menu throughout the game. My personal favorite song from the game, of course. 

## How the World Is Built

The island is generated in layers. The ocean fills the outer boundary, a sand ring surrounds the interior, and grass covers the playable area. Named facilities including Nook's Cranny, the Museum, Able's, the Plaza, the Airport, the Hotel, and occasionally Redd's are placed with collision detection and spacing buffers. **BFS** then connects every facility pair with hallways, guaranteeing all floor tiles are reachable. Tree tiles border every floor tile to form natural walls. Fruit trees and flowers are scattered randomly as finishing touches.

## Tech Stack

Java, StdDraw (Princeton algs4), Breadth First Search for hallway generation and click to path navigation, seeded pseudorandom generation via Java's Random class.

## Running the Project

Clone the repo and open it in IntelliJ. Add library-sp26 to your classpath which includes StdDraw and StdAudio. Run Main.java inside the core package.
