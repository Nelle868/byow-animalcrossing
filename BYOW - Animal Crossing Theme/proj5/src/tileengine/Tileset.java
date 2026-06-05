package tileengine;

import java.awt.Color;

/**
 * Contains constant tile objects, to avoid having to remake the same tiles in different parts of
 * the code.
 *
 * You are free to (and encouraged to) create and add your own tiles to this file. This file will
 * be turned in with the rest of your code.
 *
 * Ex:
 *      world[x][y] = Tileset.FLOOR;
 *
 * The style checker may crash when you try to style check this file due to use of unicode
 * characters. This is OK.
 */

public class Tileset {
    public static final TETile AVATAR = new TETile('☺', Color.white, Color.black, "villager", "src/avatar/marshall2.png", 0);
    public static final TETile WALL = new TETile('#', new Color(216, 128, 128), Color.darkGray,
            "Wall", 1);
    public static final TETile FLOOR = new TETile('·', new Color(128, 192, 128), new Color(101, 67, 33), "Floor", 2);
    public static final TETile NOTHING = new TETile(' ', Color.black, Color.black, "Nothing", 3);
    public static final TETile GRASS = new TETile('"', new Color(34, 139, 34), Color.green, "Grass", 4);
    public static final TETile WATER = new TETile('≈', new Color(0, 220, 220), new Color(0, 160, 160), "Water", 5);
    public static final TETile FLOWER = new TETile('❀', Color.magenta, Color.pink, "Flower", 6);
    public static final TETile LOCKED_DOOR = new TETile('█', Color.orange, Color.black,
            "Locked door", 7);
    public static final TETile UNLOCKED_DOOR = new TETile('▢', Color.orange, Color.black,
            "Unlocked door", 8);
    public static final TETile SAND = new TETile('▒', new Color(210, 180, 140), new Color(160, 130, 90), "Sand", 9);
    public static final TETile MOUNTAIN = new TETile('▲', Color.gray, Color.black, "Mountain", 10);
    public static final TETile TREE = new TETile('♠', Color.green, Color.black, "Tree", 11);

    public static final TETile CELL = new TETile('█', Color.white, Color.black, "Cell", 12);
    // Added
    public static final TETile PATH = new TETile('·', new Color(255, 182, 193), new Color(255, 105, 180), "Path", 13); // PATH is the tile used to display the pink trail between the villager and the clicked tile

   // public static final TETile CELL = new TETile('█', Color.white, Color.black, "cell", 12);
    // Added to the Tileset for the ambition feature
    // public static final TETile APPLE_TREE = new TETile('♣', new Color(220, 50, 47), Color.black, "apple tree", 13, "Documents/Class/cs61b/sp26-proj5-g362/proj5/src/tileengine/apple_tree.png");
    public static final TETile APPLE_TREE = new TETile('♣', new Color(220, 50, 47), Color.black, "apple tree", "src/tileengine/apple_tree.png", 13);
}


