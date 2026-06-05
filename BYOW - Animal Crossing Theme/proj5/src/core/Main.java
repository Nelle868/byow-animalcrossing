package core;

import tileengine.TERenderer;
import edu.princeton.cs.algs4.StdDraw;
import java.awt.Color;
import java.awt.Font;
import edu.princeton.cs.algs4.StdAudio;

public class Main {
    private static TERenderer tileRenderer = new TERenderer();
    private static World world;
    private static boolean musicPlaying = true;                             // musicPlaying controls whether the music loop should continue


    public static void main(String[] args) {
        // Hard-code seed for 5B. Change this to test different worlds.
        // Template seed: 12345L
        // Random Generated Seeds:
        //  4708285938637876482L - Screenshot Done
        //  2410978264838250863L - Screenshot Done
        //  3475098633838972279L - Screenshot Done
        //  1463031263087813029L - Screenshot Done
        //  7653382631906062139L - Screenshot Done
        //long seed = 12345L;
        tileRenderer.initialize(World.WIDTH, World.HEIGHT + 2);          // +2 to support the HUD
        playMusic("src/music/Bubblegum KK.wav"); // or DJ KK.wav
        long menuResult = showMainMenu();
        musicPlaying = false;
//        StdDraw.pause(500);                                              // wait 500ms for the menu music thread to fully stop before starting game music
//        playMusic("src/music/Bubblegum KK.wav");


        if (menuResult == -2) {
            long savedSeed = World.loadSeed();                              // savedSeed is the seed stored in save.txt
            world = new World(savedSeed);
            world.restoreState();
        } else {
            world = new World(menuResult);
        }



        // --- Gameplay ---
        boolean play = true;
        boolean mouseWasPressed = false;

        while (play) {
            while (StdDraw.hasNextKeyTyped()) {
                char lastKey = Character.toLowerCase(StdDraw.nextKeyTyped());               // lastKey is the most recent key the user typed
                if (lastKey == 'w' || lastKey == 'a' || lastKey == 's' || lastKey == 'd') {
                    world.moveVillager(lastKey);
                } else if (lastKey == ':') {
                    while (!StdDraw.hasNextKeyTyped()) {
                        StdDraw.pause(10);  // wait until the next key is available
                    }
                    char nextKey = Character.toLowerCase(StdDraw.nextKeyTyped());           // nextKey is the key pressed after the colon
                    if (nextKey == 'q') {
                        world.saveGame();
                        System.exit(0);
                    }
                } else if (lastKey == 'u') {
                    world.undoMove();                                                       // undo the most recent villager movement
                } else if (lastKey == 'k') {
                    world.redoMove();                                                       // redo the most recently undone movement
                } else if (lastKey == 'r') {
                    musicPlaying = false;                                                   // stop current music before restarting
                    main(args);                                                             // restart — return to main menu without saving
                }
            }

            boolean mouseIsPressed = StdDraw.isMousePressed();  // mouseIsPressed is true if the mouse button is currently held down

            if (mouseIsPressed && !mouseWasPressed) {           // only trigger on the first frame the mouse is pressed, not while held

                int clickedTileX = (int) StdDraw.mouseX();      // clickedTileX is the x coordinate of the tile the user clicked
                int clickedTileY = (int) StdDraw.mouseY();      // clickedTileY is the y coordinate of the tile the user clicked

                if (world.hasPathDrawn() && clickedTileX == world.getPathTargetX() && clickedTileY == world.getPathTargetY()) {
                    // Same tile clicked twice — animate the villager step by step along the path
                    for (int[] tile : world.getCurrentPath()) {
                        int nextTileX = tile[0];                // nextTileX is the x coordinate of the next tile the villager will move to
                        int nextTileY = tile[1];                // nextTileY is the y coordinate of the next tile the villager will move to
                        world.stepVillagerTo(nextTileX, nextTileY);
                        StdDraw.clear(Color.BLACK);
                        tileRenderer.drawTiles(world.getTiles());
                        drawHUD(world);
                        StdDraw.show();
                        StdDraw.pause(150);                 // pause 150 milliseconds between each step so the animation is visible
                    }
                    world.clearCurrentPath();
                } else {
                    // New tile clicked — draw path to it
                    world.handleClick(clickedTileX, clickedTileY);
                }
                StdDraw.pause(200);                         // pause to avoid registering multiple clicks from one press
            }
            StdDraw.clear(Color.BLACK);
            tileRenderer.drawTiles(world.getTiles());
            drawHUD(world);
            StdDraw.show();
            mouseWasPressed = mouseIsPressed;
            // Add a pause to avoid freezing
            StdDraw.pause(20);


        }

    }

    // --- Task 1 ---
    // Displays the main menu and waits for the user to press N, L, or Q.
    // Returns the seed the user entered if they press N.
    // Returns -1 if they press L (load game — not yet implemented).
    private static long showMainMenu() {
        StdDraw.clear(Color.BLACK);
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.setFont(new Font("Monaco", Font.BOLD, 30));
        StdDraw.text(World.WIDTH / 2.0, World.HEIGHT / 2.0 + 10, "CS61B: BYOW");    // title text centered on screen
        StdDraw.setFont(new Font("Monaco", Font.BOLD, 20));
        StdDraw.text(World.WIDTH / 2.0, World.HEIGHT / 2.0 + 3, "(N) New Game");
        StdDraw.text(World.WIDTH / 2.0, World.HEIGHT / 2.0, "(L) Load Game");
        StdDraw.text(World.WIDTH / 2.0, World.HEIGHT / 2.0 - 3, "(Q) Quit Game");
        StdDraw.show();

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char keyPressed = Character.toLowerCase(StdDraw.nextKeyTyped());                // keyPressed is the key the user pressed on the main menu
                if (keyPressed == 'n') {
                    musicPlaying = false;
                    return showSeedScreen();                                                    // take the user to the seed entry screen
                } else if (keyPressed == 'l') {
                    musicPlaying = false;
                    return -2;                                                                  // load game
                } else if (keyPressed == 'q') {
                    musicPlaying = false;
                    System.exit(0);                                                      // quit
                }
            }
        }
    }

    // Displays the seed entry screen and waits for the user to type a seed and press S.
    // Returns the seed the user entered as a long.
    private static long showSeedScreen() {
        StringBuilder seedInput = new StringBuilder();  // seedInput accumulates the digits the user has typed so far

        while (true) {
            StdDraw.clear(Color.BLACK);
            StdDraw.setPenColor(Color.WHITE);
            StdDraw.setFont(new Font("Monaco", Font.BOLD, 20));
            StdDraw.text(World.WIDTH / 2.0, World.HEIGHT / 2.0 + 3, "Enter Seed:");
            StdDraw.text(World.WIDTH / 2.0, World.HEIGHT / 2.0, seedInput.toString());     // display the digits typed so far
            StdDraw.text(World.WIDTH / 2.0, World.HEIGHT / 2.0 - 3, "Press S to start");
            StdDraw.show();

            if (StdDraw.hasNextKeyTyped()) {
                char keyPressed = Character.toLowerCase(StdDraw.nextKeyTyped());    // keyPressed is the most recent key the user typed on the seed screen
                if (keyPressed == 's' && seedInput.length() > 0) {
                    return Long.parseLong(seedInput.toString());     // parse the accumulated digits as a long and return it
                } else if (Character.isDigit(keyPressed)) {
                    seedInput.append(keyPressed);   // add the digit to the seed being built
                }
            }
        }
    }

    // --- Task 3 ---
    private static void drawHUD(World world) {
        double mousePosition_X = StdDraw.mouseX();
        double mousePosition_Y = StdDraw.mouseY();

        int hoverX = (int) mousePosition_X;
        int hoverY = (int) mousePosition_Y;

        String facilityName = world.tileDescription(hoverX, hoverY);
        StdDraw.setPenColor(Color.pink);
        StdDraw.setFont(new Font("Monaco", Font.BOLD, 20)); // to match the renderer's font
        StdDraw.textLeft(1, World.HEIGHT +1, facilityName);      // Puts the facility name in the upper left of the HUD
    }

    // ---- Ambition Helper (Audio) ----

    // Plays the given audio file on loop continuously in a separate thread so it doesn't block the game loop
    private static void playMusic(String filePath) {    // filePath is the relative path to the audio file
        new Thread(() -> {
            while (musicPlaying) {
                StdAudio.play(filePath);
            }
        }).start();
    }
}
