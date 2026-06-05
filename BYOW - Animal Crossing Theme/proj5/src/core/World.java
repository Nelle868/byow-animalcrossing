package core;

import edu.princeton.cs.algs4.StdDraw;
import tileengine.TETile;
import tileengine.Tileset;
import utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Arrays;

public class World {

    // World boundary
    public static final int WIDTH = 80;
    public static final int HEIGHT = 50;

    // Sand island boundary (ocean outside, sand inside)
    private static final int SAND_X = 4;
    private static final int SAND_Y = 4;
    private static final int SAND_W = WIDTH - 8;
    private static final int SAND_H = HEIGHT - 8;

    // Grass interior boundary (sand outside, grass inside)
    private static final int GRASS_X = SAND_X + 4;
    private static final int GRASS_Y = SAND_Y + 4;
    private static final int GRASS_W = SAND_W - 8;
    private static final int GRASS_H = SAND_H - 8;

    private TETile[][] islandGrid;                                  // 2D array of the world
    private Random random_seed;                                     // Random seeds to generate the world
    private List<Room> facilitiesList;                              // "Rooms" are the island's facilities. This list contains each of them.
    private int villagerPosition_X;                                 // villagerPosition_X and villagerPosition_Y track the coordinate position of the villager on the island grid.
    private int villagerPosition_Y;

    // --- For Task 5 ---
    private List <int[]> currentPath = new ArrayList <>();          // Tile coordinates in the current path
    private boolean pathIsDrawn = false;                            // True if displayed on screen
    private int pathTarget_X = -1;                                  // pathTarget_X and pathTarget_Y are the intended end point on the island grid
    private int pathTarget_Y = -1;

    // --- For Task 4 ---
    private long worldSeed;                                         // worldSeed stores the seed used to generate this world, needed for saving

    // --- Ambition Features ---
    private List<int[]> moveHistory = new ArrayList<>();            // moveHistory stores the list of villager positions before each move, used for undo
    private List<int[]> redoHistory = new ArrayList<>();            // redoHistory stores the list of villager positions that were undone, used for redo

    // Constructor
    public World(long seed) {
        this.worldSeed = seed;
        islandGrid = new TETile[WIDTH][HEIGHT];
        random_seed = new Random(seed);
        facilitiesList = new ArrayList<>();
        generateWorld();
        placeVillager();                                            // Puts the villager on the island
    }

    // --- Main generation pipeline ---

    private void generateWorld() {
        fillBackground();
        placeFacilities();
        connectAllRooms();
        placeWalls();
        placeAppleTrees();
    }

    // Step 1: Fill ocean, sand, and grass layers
    // Flowers appear randomly on grass tiles with a 10% chance
    private static final double FLOWER_CHANCE = 0.10;

    private void fillBackground() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (inGrass(x, y)) {
                    if (random_seed.nextDouble() < FLOWER_CHANCE) {
                        islandGrid[x][y] = Tileset.FLOWER;
                    } else {
                        islandGrid[x][y] = Tileset.GRASS;
                    }
                } else if (inSand(x, y)) {
                    islandGrid[x][y] = Tileset.SAND;
                } else {
                    islandGrid[x][y] = Tileset.WATER;
                }
            }
        }
    }

    // Step 2: Place all named facilities
    private void placeFacilities() {
        // Interior facilities (random positions on grass)
        placeInteriorRoom("Nook's Cranny", 6, 4);
        placeInteriorRoom("Museum", 8, 5);
        placeInteriorRoom("Able's", 5, 4);
        placeInteriorRoom("Plaza", 7, 5);

        // Beach edge facilities
        placeSouthWaterRoom("Airport", 6, 3);
        placeEastWaterRoom("Hotel", 3, 5);

        // Hotel appears 40% of the time on the east beach
        if (random_seed.nextDouble() < 0.4) {
            placeNorthWaterRoom("Redd's", 5, 3);
        }
    }

    // Step 3: Connect all rooms sequentially with L-shaped hallways
    private void connectAllRooms() {
        for (int i = 0; i < facilitiesList.size() - 1; i++) {
            drawHallway(facilitiesList.get(i), facilitiesList.get(i + 1));
        }
    }

    // Step 4: Place tree tiles around all floor tiles
    private void placeWalls() {
        int[] neighborSteps_X = {-1, 0, 1, -1, 1, -1, 0, 1};                                                        // Array of horizontal distance from the current tile to its 8 neighboring tiles.
        int[] neighborSteps_Y = {-1, -1, -1, 0, 0, 1, 1, 1};                                                        // Array of vertical distance from the current tile to its 8 neighboring tiles.

        for (int currentTile_X = 0; currentTile_X < WIDTH; currentTile_X++) {                                       // currentTile_X is the x coordinate of the current tile
            for (int currentTile_Y = 0; currentTile_Y < HEIGHT; currentTile_Y++) {                                  // currentTile_Y is the x coordinate of the current tile

                boolean isGrassTile = inGrass(currentTile_X, currentTile_Y);                                       // isGrassTile is true if the current tile is in the grass interior
                boolean isTreeTile = islandGrid[currentTile_X][currentTile_Y].equals(Tileset.TREE);                // isTreeTile is true if the current tile is already a tree
                boolean isFloorTile = islandGrid[currentTile_X][currentTile_Y].equals(Tileset.FLOOR);              // isFloorTile is true if the current tile is a floor tile

                if (isGrassTile && !isTreeTile && !isFloorTile) {
                    for (int directionIdx = 0; directionIdx < 8; directionIdx++) {                                 // directionIdx tracks which of the 8 neighboring directions is currently being checked
                        int neighborTile_X = currentTile_X + neighborSteps_X[directionIdx];                        // neighborTile_X is the x coordinate of the neighboring tile currently being examined
                        int neighborTile_Y = currentTile_Y + neighborSteps_Y[directionIdx];                        // neighborTile_Y is the y coordinate of the neighboring tile currently being examined
                        if (inBounds(neighborTile_X, neighborTile_Y) && islandGrid[neighborTile_X][neighborTile_Y].equals(Tileset.FLOOR)) {
                            islandGrid[currentTile_X][currentTile_Y] = Tileset.TREE;
                        }
                    }
                }
            }
        }
    }

    // --- Room placement helpers ---

    // Try up to 200 times to find a non-overlapping spot on the grass
    private void placeInteriorRoom(String name, int facilityWidth, int facilityHeight) {                                // facilityWidth is the width of the facility to be placed
        for (int placementAttempt = 0; placementAttempt < 200; placementAttempt++) {                                    // placementAttempt is the number of times we looked for an open position to place the facility                                                              // facilityHeight is the height of the facility to be placed
            int randomFacility_X = GRASS_X + 1 + random_seed.nextInt(Math.max(1, GRASS_W - facilityWidth - 2));         // randomFacility_X is the x-coordinate for the top left corner of the facility being placed
            int randomFacility_Y = GRASS_Y + 1 + random_seed.nextInt(Math.max(1, GRASS_H - facilityHeight - 2));        // randomFacility_Y is the y-coordinate for the top left corner of the facility being placed
            Room candidate = new Room(name, randomFacility_X, randomFacility_Y, facilityWidth, facilityHeight);
            if (!overlapsAny(candidate)) {
                facilitiesList.add(candidate);
                fillRoom(candidate);
                return;
            }
        }
        // If no valid position found after 200 tries, skip this room
    }

    // Note: x and y in the following methods represent points/tiles on the 2D grid

    // Place a room on the south water (below the island)
    private void placeSouthWaterRoom(String name, int intendedWidth, int intendedHeight) {
        int x = 1 + random_seed.nextInt(Math.max(1, WIDTH - intendedWidth - 2));
        int y = 1;
        Room room = new Room(name, x, y, intendedWidth, intendedHeight);
        facilitiesList.add(room);
        fillRoom(room);
    }

    // Place a room on the north water (above the island)
    private void placeNorthWaterRoom(String name, int intendedWidth, int intendedHeight) {
        int x = 1 + random_seed.nextInt(Math.max(1, WIDTH - intendedWidth - 2));
        int y = HEIGHT - intendedHeight - 1;
        Room room = new Room(name, x, y, intendedWidth, intendedHeight);
        if (!overlapsAny(room)) {
            facilitiesList.add(room);
            fillRoom(room);
        }
    }

    // Place a room on the east water (right of the island)
    private void placeEastWaterRoom(String name, int intendedWidth, int intendedHeight) {
        int x = WIDTH - intendedWidth - 1;
        int y = 1 + random_seed.nextInt(Math.max(1, HEIGHT - intendedHeight - 2));
        Room room = new Room(name, x, y, intendedWidth, intendedHeight);
        if (!overlapsAny(room)) {
            facilitiesList.add(room);
            fillRoom(room);
        }
    }

    // Fill all tiles in a room with FLOOR
    private void fillRoom(Room facility) {
        for (int x = facility.x; x < facility.x + facility.width; x++) {
            for (int y = facility.y; y < facility.y + facility.height; y++) {
                if (inBounds(x, y)) {
                    islandGrid[x][y] = Tileset.FLOOR;
                }
            }
        }
    }

    // Check if a candidate room overlaps any existing room (with 2-tile buffer)
    private boolean overlapsAny(Room candidate) {
        for (Room facility : facilitiesList) {
            if (candidate.overlaps(facility, 2)) {
                return true;
            }
        }
        return false;
    }

    // --- The Villager ---

    private void placeVillager() {                                                        // Finds the bottom-leftmost tile and places the villager there
        for (int possibleTile_X = 0; possibleTile_X < WIDTH; possibleTile_X++) {          // possibleTile_X is the x coordinate of the tile currently being checked to possible place the villager there
            for (int possibleTile_Y = 0; possibleTile_Y < HEIGHT; possibleTile_Y++) {     // possibleTile_Y is the y coordinate of the tile currently being checked to possible place the villager there
                if (islandGrid[possibleTile_X][possibleTile_Y].equals(Tileset.FLOOR)) {
                    villagerPosition_X = possibleTile_X;
                    villagerPosition_Y = possibleTile_Y;
                    islandGrid[villagerPosition_X][villagerPosition_Y] = Tileset.AVATAR;
                    return;
                }
            }
        }
    }

    public void moveVillager(char key) {                                                // Moves the villager in the given direction if the intended tile is a walkable tile
        int intended_X = villagerPosition_X;
        int intended_Y = villagerPosition_Y;

        if (key == 'w') {
            intended_Y++;
        } else if (key == 's') {
            intended_Y--;
        } else if (key == 'd') {
            intended_X++;
        } else if (key == 'a') {
            intended_X--;
        }

        // Limit movement to walkable tiles only
        if (inBounds(intended_X, intended_Y) && islandGrid[intended_X][intended_Y].equals(Tileset.FLOOR)) {
            moveHistory.add(new int[]{villagerPosition_X, villagerPosition_Y});         // save current position before moving
            redoHistory.clear();                                                        // clear redo history when a new move is made
            islandGrid[villagerPosition_X][villagerPosition_Y] = Tileset.FLOOR;
            villagerPosition_X = intended_X;
            villagerPosition_Y = intended_Y;
            islandGrid[villagerPosition_X][villagerPosition_Y] = Tileset.AVATAR;
        }
    }

    // --- Villager Getters ---

    public int getVillagerPosition_X() {
        return villagerPosition_X;
    }

    public int getVillagerPosition_Y() {
        return villagerPosition_Y;
    }

    // --- Ambition Feature Methods ---
    // Undoes the most recent villager movement by restoring the previous position
    public void undoMove() {
        if (!moveHistory.isEmpty()) {
            int[] previousPosition = moveHistory.remove(moveHistory.size() - 1);        // previousPosition is the villager's position before the last move
            redoHistory.add(new int[]{villagerPosition_X, villagerPosition_Y});               // save current position to redo history
            islandGrid[villagerPosition_X][villagerPosition_Y] = Tileset.FLOOR;
            villagerPosition_X = previousPosition[0];
            villagerPosition_Y = previousPosition[1];
            islandGrid[villagerPosition_X][villagerPosition_Y] = Tileset.AVATAR;
        }
    }

    // Redoes the most recently undone villager movement
    public void redoMove() {
        if (!redoHistory.isEmpty()) {
            int[] nextPosition = redoHistory.remove(redoHistory.size() - 1);            // nextPosition is the villager's position that was undone
            moveHistory.add(new int[]{villagerPosition_X, villagerPosition_Y});               // save current position to move history
            islandGrid[villagerPosition_X][villagerPosition_Y] = Tileset.FLOOR;
            villagerPosition_X = nextPosition[0];
            villagerPosition_Y = nextPosition[1];
            islandGrid[villagerPosition_X][villagerPosition_Y] = Tileset.AVATAR;
        }
    }

    // --- Task 4 and Ambition Features ---
    public void saveGame() {                                                        // Saves the game state
        StringBuilder gameState = new StringBuilder();
        gameState.append(worldSeed).append("\n")
                .append(villagerPosition_X).append("\n")
                .append(villagerPosition_Y).append("\n")
                .append(pathTarget_X).append("\n")
                .append(pathTarget_Y).append("\n")
                .append(pathIsDrawn).append("\n")
                .append(moveHistory.size()).append("\n");                           // save the number of moves in history
        for (int[] move : moveHistory) {
            gameState.append(move[0]).append(",").append(move[1]).append("\n");     // save each move as x,y
        }
        FileUtils.writeFile("save.txt", gameState.toString());
    }

    public static long loadSeed() {                                                 // Loads a previous game
        String[] lines = FileUtils.readFile("save.txt").split("\n"); // Lines contains each piece of saved data
        return Long.parseLong(lines[0]);                                            // Return just the seed so Main.java can construct the world
    }

    public void restoreState() {
        String[] lines = FileUtils.readFile("save.txt").split("\n"); // Lines contains each piece of saved data
        int savedVillagerX = Integer.parseInt(lines[1]);                            // savedVillagerX is the villager's x position when saved
        int savedVillagerY = Integer.parseInt(lines[2]);                            // savedVillagerY is the villager's y position when saved
        int savedPathTargetX = Integer.parseInt(lines[3]);                          // savedPathTargetX is the path target x when saved
        int savedPathTargetY = Integer.parseInt(lines[4]);                          // savedPathTargetY is the path target y when saved
        boolean savedPathIsDrawn = Boolean.parseBoolean(lines[5]);                  // savedPathIsDrawn is true if a path was displayed when saved
        if (lines.length > 6) {
            int moveHistorySize = Integer.parseInt(lines[6]);                       // moveHistorySize is the number of moves stored in the history
            for (int i = 0; i < moveHistorySize; i++) {
                String[] coords = lines[7 + i].split(",");                    // coords contains the x and y of each saved move
                moveHistory.add(new int[]{Integer.parseInt(coords[0]), Integer.parseInt(coords[1])});
            }
        }

        islandGrid[villagerPosition_X][villagerPosition_Y] = Tileset.FLOOR;
        villagerPosition_X = savedVillagerX;
        villagerPosition_Y = savedVillagerY;
        islandGrid[villagerPosition_X][villagerPosition_Y] = Tileset.AVATAR;

        if (savedPathIsDrawn) {
            List<int[]> restoredPath = findPath(villagerPosition_X, villagerPosition_Y, savedPathTargetX, savedPathTargetY);
            if (!restoredPath.isEmpty()) {
                currentPath = restoredPath;
                pathTarget_X = savedPathTargetX;
                pathTarget_Y = savedPathTargetY;
                drawPath();
                pathIsDrawn = true;
            }
        }
    }

    // --- Task 5 ---

    public boolean hasPathDrawn() {
        return pathIsDrawn;                                                         // Returns true if a path is currently displayed on the island
    }

    public int getPathTargetX() {
        return pathTarget_X;                                                        // Returns the x coordinate of the tile the villager is pathfinding to
    }

    public int getPathTargetY() {
        return pathTarget_Y;                                                        // Returns the y coordinate of the tile the villager is pathfinding to
    }

    public List<int[]> getCurrentPath() {
        return currentPath;                                                         // Returns the list of tile coordinates making up the current path
    }

    // Moves the villager one step to the given tile coordinates
    // Used by the animation loop in Main.java to move the villager step by step
    public void stepVillagerTo(int nextTileX, int nextTileY) {                      // nextTileX and nextTileY are the coordinates of the tile the villager is moving to
        islandGrid[villagerPosition_X][villagerPosition_Y] = Tileset.FLOOR;
        villagerPosition_X = nextTileX;
        villagerPosition_Y = nextTileY;
        islandGrid[villagerPosition_X][villagerPosition_Y] = Tileset.AVATAR;
    }

    // Handles a mouse click at the given tile coordinates.
    // Clears any existing path and draws a new path to the clicked tile.
    // The animation logic lives in Main.java and triggers when the same tile is clicked TWICE.
    public void handleClick(int clickedTileX, int clickedTileY) {                   // clickedTileX and clickedTileY are the coordinates of the tile the user clicked
        if (!inBounds(clickedTileX, clickedTileY)) {
            return;
        }
        // Clear any previously drawn path before drawing a new one
        clearCurrentPath();
        List<int[]> foundPath = findPath(villagerPosition_X, villagerPosition_Y, clickedTileX, clickedTileY);   // foundPath is the shortest path from the villager to the clicked tile
        if (!foundPath.isEmpty()) {
            currentPath = foundPath;
            pathTarget_X = clickedTileX;                                                                        // record the target tile so Main.java knows when the same tile is clicked again
            pathTarget_Y = clickedTileY;
            drawPath();
            pathIsDrawn = true;
        }
    }

    // Uses BFS to find the shortest path from the start tile to the end tile
    // walking only on floor and avatar tiles.
    // Returns a list of tile coordinates representing the path, from start to end.
    private List<int[]> findPath(int current_X, int current_Y, int destination_X, int destination_Y) {          // current_X and current_Y are the villager's current position. destination_X, destination_Y are the target tile coordinates
        boolean[][] tilesCheckedBFS = new boolean[WIDTH][HEIGHT];                                               // tilesCheckedBFS tracks which tiles BFS has already examined
        int[][] previousTile_X = new int[WIDTH][HEIGHT];                                                        // previousTile_X and previousTile_Y store the breadcrumb trail
        int[][] previousTile_Y = new int[WIDTH][HEIGHT];

        for (int[] row : previousTile_X) {
            Arrays.fill(row, -1);
        }
        for (int[] row : previousTile_Y) {
            Arrays.fill(row, -1);
        }

        Queue<int[]> bfsQueue = new LinkedList<>();
        bfsQueue.add(new int[]{current_X, current_Y});
        tilesCheckedBFS[current_X][current_Y] = true;

        int[] direction_X = {0, 0, 1, -1};                                                                      // direction_X and direction_Y are the distances to the 4 non-diagonal neighboring tiles
        int[] direction_Y = {1, -1, 0, 0};

        boolean pathExists = false;

        while (!bfsQueue.isEmpty()) {
            int[] currentTile = bfsQueue.poll();
            int currentTile_X = currentTile[0];                                                                 // currentTile_X is the x coordinate of the tile currently being processed by BFS
            int currentTile_Y = currentTile[1];                                                                 // currentTile_Y is the y coordinate of the tile currently being processed by BFS

            if (currentTile_X == destination_X && currentTile_Y == destination_Y) {
                pathExists = true;
            }

            if (!pathExists) {
                for (int directionIdx = 0; directionIdx < 4; directionIdx++) {
                    int possibleTile_X = currentTile_X + direction_X[directionIdx];                             // possibleTile_X is the x coordinate of the neighboring tile being evaluated
                    int possibleTile_Y = currentTile_Y + direction_Y[directionIdx];                             // possibleTile_Y is the y coordinate of the neighboring tile being evaluated
                    boolean isTileWalkable = islandGrid[possibleTile_X][possibleTile_Y].equals(Tileset.FLOOR)
                            || islandGrid[possibleTile_X][possibleTile_Y].equals(Tileset.AVATAR);               // isTileWalkable is true if the tile is a floor or avatar tile
                    if (inBounds(possibleTile_X, possibleTile_Y) && !tilesCheckedBFS[possibleTile_X][possibleTile_Y] && isTileWalkable) {
                        tilesCheckedBFS[possibleTile_X][possibleTile_Y] = true;
                        previousTile_X[possibleTile_X][possibleTile_Y] = currentTile_X;
                        previousTile_Y[possibleTile_X][possibleTile_Y] = currentTile_Y;
                        bfsQueue.add(new int[]{possibleTile_X, possibleTile_Y});
                    }
                }
            }
        }

        // Trace the breadcrumb trail backwards to build the path
        List<int[]> path = new ArrayList<>();
        if (pathExists) {
            int currentTile_X = destination_X;
            int currentTile_Y = destination_Y;
            while (currentTile_X != current_X || currentTile_Y != current_Y) {
                path.add(0, new int[]{currentTile_X, currentTile_Y});                                   // add to front so path goes from start to end
                int previous_X = previousTile_X[currentTile_X][currentTile_Y];
                int previous_Y = previousTile_Y[currentTile_X][currentTile_Y];
                currentTile_X = previous_X;
                currentTile_Y = previous_Y;
            }
        }
        return path;
    }

    // Draws the pink PATH tiles along the current path
    private void drawPath() {
        for (int[] tile : currentPath) {
            int pathTileX = tile[0];                                                                        // pathTileX is the x coordinate of the path tile being drawn
            int pathTileY = tile[1];                                                                        // pathTileY is the y coordinate of the path tile being drawn
            if (!islandGrid[pathTileX][pathTileY].equals(Tileset.AVATAR)) {
                islandGrid[pathTileX][pathTileY] = Tileset.PATH;
            }
        }
    }

    // Clears the currently displayed path by restoring all PATH tiles back to FLOOR
    public void clearCurrentPath() {
        for (int[] tile : currentPath) {
            int pathTileX = tile[0];                                                                        // pathTileX is the x coordinate of the path tile being cleared
            int pathTileY = tile[1];                                                                        // pathTileY is the y coordinate of the path tile being cleared
            if (!islandGrid[pathTileX][pathTileY].equals(Tileset.AVATAR)) {
                islandGrid[pathTileX][pathTileY] = Tileset.FLOOR;
            }
        }
        currentPath.clear();
        pathIsDrawn = false;
        pathTarget_X = -1;
        pathTarget_Y = -1;
    }

    // --- Hallway drawing ---

    // L-shaped hallway: horizontal first, then vertical
    private void drawHallway(Room beginningFacility, Room destinationFacility) {
        // Search outward from the facility center with an expanding radius
        // until we find a valid grass or
        // sand tile to use as the hallway start point.

        int hallwayStartX = beginningFacility.centerX();
        int hallwayStartY = beginningFacility.centerY();
        // Find a starting point of hallway on grass or sand tile
        findHallwayStart:
        for (int searchRadius = 0; searchRadius < Math.max(WIDTH, HEIGHT); searchRadius++) {                // searchRadius checks how far out from the facility center we are currently searching for a valid grass or sand tile
            for (int search_X = -searchRadius; search_X <= searchRadius; search_X++) {                      // search_X and search_Y are the coordinates the tiles we are broadly checking
                for (int search_Y = -searchRadius; search_Y <= searchRadius; search_Y++) {
                    if (Math.abs(search_X) != searchRadius && Math.abs(search_Y) != searchRadius) {
                        int possibleTile_X = beginningFacility.centerX() + search_X;                        // possibleTile_X and possibleTile_Y are the coordinates of a prospective tile to start the hallway
                        int possibleTile_Y = beginningFacility.centerY() + search_Y;
                        if (inBounds(possibleTile_X, possibleTile_Y) && (inGrass(possibleTile_X, possibleTile_Y) || inSand(possibleTile_X, possibleTile_Y))) {
                            hallwayStartX = possibleTile_X;
                            hallwayStartY = possibleTile_Y;
                            break findHallwayStart;
                    }
                }
                }
            }
        }

        // Search outward from the facility center with an expanding radius
        // until we find a valid grass or
        // sand tile to use as the hallway end point.

        int hallwayEnds_X = destinationFacility.centerX();
        int hallwayEnds_Y = destinationFacility.centerY();
        findHallwayEnd:
        for (int searchRadius = 0; searchRadius < Math.max(WIDTH, HEIGHT); searchRadius++) {
            for (int search_X = -searchRadius; search_X <= searchRadius; search_X++) {
                for (int search_Y = -searchRadius; search_Y <= searchRadius; search_Y++) {
                    if (Math.abs(search_X) != searchRadius && Math.abs(search_Y) != searchRadius) {
                        int possibleTile_X = destinationFacility.centerX() + search_X;                          // possibleTile_X and possibleTile_Y are the coordinates of a prospective tile to end the hallway
                        int possibleTile_Y = destinationFacility.centerY() + search_Y;
                        if (inBounds(possibleTile_X, possibleTile_Y) && (inGrass(possibleTile_X, possibleTile_Y) || inSand(possibleTile_X, possibleTile_Y))) {
                            hallwayEnds_X = possibleTile_X;
                            hallwayEnds_Y = possibleTile_Y;
                            break findHallwayEnd;
                        }
                    }
                }
            }
        }

        // BFS to find the shortest path
        boolean[][] tilesCheckedBFS = new boolean[WIDTH][HEIGHT];
        int[][] previousTile_X = new int[WIDTH][HEIGHT];                                                        // previousTile_X and previousTile_Y tracks the path backward
        int[][] previousTile_Y = new int[WIDTH][HEIGHT];

        for (int[] row : previousTile_X) {
            Arrays.fill(row, -1);                                                                           // When we iterate over each row, we're covering all Y positions as well within each X, filling them with -1 values
        }

        Queue<int[]> bfsQueue = new LinkedList<>();
        bfsQueue.add(new int[]{hallwayStartX, hallwayStartY});
        tilesCheckedBFS[hallwayStartX][hallwayStartY] = true;

        int[] direction_X = {0, 0, 1, -1};                                                                      // direction_X and direction_Y are the distances to nondiagonal, neigboring tiles
        int[] direction_Y = {1, -1, 0, 0};

        boolean pathExists = false;                                                                             // boolean determined by BFS finding a path between 2 facilities

        while (!bfsQueue.isEmpty()) {
            int[] currenTile = bfsQueue.poll();                                                                 // current tile in BFS
            int currentTile_X = currenTile[0];
            int currentTile_Y = currenTile[1];

            if (currentTile_X == hallwayEnds_X && currentTile_Y == hallwayEnds_Y) {
                pathExists = true;
                break;
            }

            for (int directionIdx = 0; directionIdx < 4; directionIdx++) {                                      // directionIdx tracks the nondiagonal tile being checked.
                int possibleTile_X = currentTile_X + direction_X[directionIdx];
                int possibleTile_Y = currentTile_Y + direction_Y[directionIdx];
                if (inBounds(possibleTile_X, possibleTile_Y) && !tilesCheckedBFS[possibleTile_X][possibleTile_Y]
                        && (inGrass(possibleTile_X, possibleTile_Y) || inSand(possibleTile_X, possibleTile_Y))) {
                    tilesCheckedBFS[possibleTile_X][possibleTile_Y] = true;
                    previousTile_X[possibleTile_X][possibleTile_Y] = currentTile_X;
                    previousTile_Y[possibleTile_X][possibleTile_Y] = currentTile_Y;
                    bfsQueue.add(new int[]{possibleTile_X, possibleTile_Y});
                }
            }
        }

        if (pathExists) {
            int currentTile_X = hallwayEnds_X;
            int currentTile_Y = hallwayEnds_Y;
            while (currentTile_X != hallwayStartX || currentTile_Y != hallwayStartY) {
                islandGrid[currentTile_X][currentTile_Y] = Tileset.FLOOR;
                int previous_X = previousTile_X[currentTile_X][currentTile_Y];                                  // previous_X and previous_Y is the coordinate position we just came from
                int previous_Y = previousTile_Y[currentTile_X][currentTile_Y];
                currentTile_X = previous_X;
                currentTile_Y = previous_Y;
            }
            islandGrid[hallwayStartX][hallwayStartY] = Tileset.FLOOR;
        }
    }


    // --- Utility methods ---

    // Note: x and y are positions on the 2D world grid

    private boolean inSand(int x, int y) {
        return x >= SAND_X && x < SAND_X + SAND_W
                && y >= SAND_Y && y < SAND_Y + SAND_H;
    }

    private boolean inGrass(int x, int y) {
        return x >= GRASS_X && x < GRASS_X + GRASS_W
                && y >= GRASS_Y && y < GRASS_Y + GRASS_H;
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }

    public TETile[][] getTiles() {
        return islandGrid;
    }

    public String tileDescription(int tileX, int tileY) {                                                   // Returns the name of the facility at the given tile position, or the tile's default description if no facility is there
        if (!inBounds(tileX, tileY)) {
            return "";
        }
        for (Room facility : facilitiesList) {
            boolean withinFacilityX = tileX >= facility.x && tileX < facility.x + facility.width;           // withinFacilityX is true if the tile is within the horizontal bounds of the facility
            boolean withinFacilityY = tileY >= facility.y && tileY < facility.y + facility.height;          // withinFacilityY is true if the tile is within the vertical bounds of the facility
            if (withinFacilityX && withinFacilityY) {
                return facility.name;
            }
        }
        return islandGrid[tileX][tileY].description();
    }

    public List<Room> getRooms() {
        return facilitiesList;
    }

    // --- Inner class: Room ---

    static class Room {
        String name;
        int x, y, width, height;

        Room(String name, int x, int y, int width, int height) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        int centerX() {
            return x + width / 2;
        }

        int centerY() {
            return y + height / 2;
        }

        // Returns true if this room overlaps another room, including a buffer gap of 2 tiles
        boolean overlaps(Room other, int buffer) {
            return x - buffer < other.x + other.width + buffer
                    && x + width + buffer > other.x - buffer
                    && y - buffer < other.y + other.height + buffer
                    && y + height + buffer > other.y - buffer;
        }
    }

    // --- Ambition feature: Apple tree is randomly generated on grass
    // Claude created image
    private void placeAppleTrees() {
        int target = 5 + random_seed.nextInt(5);
        int placed = 0;
        int attempts = 0;
        int maxAttempts = 1000;

        while (placed < target && attempts < maxAttempts) {
            int x = random_seed.nextInt(WIDTH);
            int y = random_seed.nextInt(HEIGHT);
            if (islandGrid[x][y].equals(Tileset.GRASS)) {
                islandGrid[x][y] = Tileset.APPLE_TREE;
                placed++;
            }
            attempts++;
        }
    }

}
