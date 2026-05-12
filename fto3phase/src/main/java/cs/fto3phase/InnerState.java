package cs.fto3phase;

public class InnerState{
    int corners = SOLVED_CORNERS;
    long edges = SOLVED_EDGES;
    long centers = SOLVED_CENTERS;

    long packedCenterIndicesLow = SOLVED_CENTER_INDICES_LOW;
    long packedCenterIndicesHigh = SOLVED_CENTER_INDICES_HIGH;

    private static final int SOLVED_CORNERS;
    private static final long SOLVED_EDGES;
    private static final long SOLVED_CENTERS;
    private static final long SOLVED_CENTER_INDICES_LOW;
    private static final long SOLVED_CENTER_INDICES_HIGH;

    /**
     * Initialization of bit-hacking
     */
    private static final int CORNER_BITS = 5;
    private static final int CORNER_MASK = 0b11111;
    private static final int[] CORNER_MASKS = new int[6];
    static {
        for (int i = 0; i < 6; i++) {
            CORNER_MASKS[i] = CORNER_MASK << (CORNER_BITS * i);
        }

        int solvedCorners = 0;
        for (int i = 0; i < 6; i++) {
            solvedCorners |= encodeCorner(i, 0) << (CORNER_BITS * i);
        }
        SOLVED_CORNERS = solvedCorners;
    }

    private static final int EDGE_BITS = 4;
    private static final long EDGE_MASK = 0b1111;
    private static final long[] EDGE_MASKS = new long[12];
    static {
        for (int i = 0; i < 12; i++) {
            EDGE_MASKS[i] = EDGE_MASK << (EDGE_BITS * i);
        }

        long solvedEdges = 0;
        for (long i = 0; i < 12; i++) {
            solvedEdges |= i << (EDGE_BITS * i);
        }
        SOLVED_EDGES = solvedEdges;
    }

    private static final int CENTER_BITS = 2;
    private static final long CENTER_MASK = 0b11;
    private static final long[] CENTER_MASKS = new long[24];
    static {
        for (int i = 0; i < 24; i++) {
            CENTER_MASKS[i] = CENTER_MASK << (CENTER_BITS * i);
        }

        long solvedCenters = 0;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 3; j++){
                solvedCenters |= (long) (i & CENTER_MASK) << (CENTER_BITS * ((i * 3) + j));
            }
        }
        SOLVED_CENTERS = solvedCenters;
    }

    private static final int CENTER_INDEX_BITS = 5;
    private static final long CENTER_INDEX_MASK = 0b11111;
    private static final long[] CENTER_INDEX_MASKS = new long[12];
    static {
        for (int i = 0; i < 12; i++) {
            CENTER_INDEX_MASKS[i] = CENTER_INDEX_MASK << (CENTER_INDEX_BITS * i);
        }

        long solvedCenterIndicesLow = 0;
        long solvedCenterIndicesHigh = 0;
        for (int i = 0; i < 12; i++) {
            solvedCenterIndicesLow |= (long) i << (CENTER_INDEX_BITS * i);
            solvedCenterIndicesHigh |= (long) (i + 12) << (CENTER_INDEX_BITS * i);
        }
        SOLVED_CENTER_INDICES_LOW = solvedCenterIndicesLow;
        SOLVED_CENTER_INDICES_HIGH = solvedCenterIndicesHigh;
    }


    public InnerState(){

    }

    public InnerState(InnerState state) {
        this.corners = state.corners;
        this.edges = state.edges;
        this.centers = state.centers;
        this.packedCenterIndicesHigh =  state.packedCenterIndicesHigh;
        this.packedCenterIndicesLow = state.packedCenterIndicesLow;

    }

    public static int getCornerIndex(int corner){
        return corner >> 2;
    }

    public static int getCornerOrientation(int corner){
        return corner & 0b11;
    }

    public int getCorner(int i){
        return (corners >> (CORNER_BITS * i)) & CORNER_MASK;
    }

    public void setCorner(int i, int corner){
        int shift = CORNER_BITS * i;
        corners = (corners & ~CORNER_MASKS[i]) | (corner << shift);
    }

    public void cycleCorners(int i1, int i2, int i3){
        int c1 = getCorner(i1);
        int c2 = getCorner(i2);
        int c3 = getCorner(i3);

        setCorner(i1, c3);
        setCorner(i2, c1);
        setCorner(i3, c2);
    }

    public void swapCorners(int i1, int i2){
        int c1 = getCorner(i1);
        int c2 = getCorner(i2);

        setCorner(i1, c2);
        setCorner(i2, c1);
    }

    public void twistCorner(int i, int dir){
        int corner = getCorner(i);
        setCorner(i, encodeCorner(getCornerIndex(corner), (getCornerOrientation(corner) + dir) & 0b11));
    }

    int getEdge(int i){
        return (int) ((edges >> (EDGE_BITS * i)) & EDGE_MASK);
    }

    private void setEdge(int i, int edge){
        int shift = EDGE_BITS * i;
        edges = (edges & ~EDGE_MASKS[i]) | ((long) edge << shift);
    }

    public void cycleEdges(int i1, int i2, int i3){
        int e1 = getEdge(i1);
        int e2 = getEdge(i2);
        int e3 = getEdge(i3);

        setEdge(i1, e3);
        setEdge(i2, e1);
        setEdge(i3, e2);
    }

    public void swapEdges(int i1, int i2){
        int e1 = getEdge(i1);
        int e2 = getEdge(i2);

        setEdge(i1, e2);
        setEdge(i2, e1);
    }

    private int getCenter(int i){
        return (int) ((centers >> (CENTER_BITS * i)) & CENTER_MASK);
    }

    public void setCenter(int i, int center){
        int shift = CENTER_BITS * i;
        centers = (centers & ~CENTER_MASKS[i]) | ((long) center << shift);
    }

    private static int encodeCorner(int perm, int orientation){
        return ((perm << 2) | orientation);
    }


    /**
     * Get center index in centerIndices
     * @param i
     * @return
     */
    int getCenterIndex(int i){
        int packedIndex = i % 12;
        long packed = i < 12 ? packedCenterIndicesLow : packedCenterIndicesHigh;
        return (int) ((packed >> (CENTER_INDEX_BITS * packedIndex)) & CENTER_INDEX_MASK);
    }

    private void setCenterIndex(int i, int centerIndex){
        int packedIndex = i % 12;
        int shift = CENTER_INDEX_BITS * packedIndex;
        if (i < 12) {
            packedCenterIndicesLow = (packedCenterIndicesLow & ~CENTER_INDEX_MASKS[packedIndex]) | ((long) centerIndex << shift);
        } else {
            packedCenterIndicesHigh = (packedCenterIndicesHigh & ~CENTER_INDEX_MASKS[packedIndex]) | ((long) centerIndex << shift);
        }
    }

    void cycleThreeCenters(int i1, int i2, int i3){
        int c1 = getCenter(i1);
        int c2 = getCenter(i2);
        int c3 = getCenter(i3);

        setCenter(i1, c3);
        setCenter(i2, c1);
        setCenter(i3, c2);

        int ci1 = getCenterIndex(i1);
        int ci2 = getCenterIndex(i2);
        int ci3 = getCenterIndex(i3);
        setCenterIndex(i1, ci3);
        setCenterIndex(i2, ci1);
        setCenterIndex(i3, ci2);
    }

    void swapCenters(int i1, int i2){
        int c1 = getCenter(i1);
        int c2 = getCenter(i2);

        setCenter(i1, c2);
        setCenter(i2, c1);

        int ci1 = getCenterIndex(i1);
        int ci2 = getCenterIndex(i2);
        setCenterIndex(i1, ci2);
        setCenterIndex(i2, ci1);
    }

    int getCenterOrdinal(int index){
        return (int)(centers >> (2 * index) & CENTER_MASK) + (index > 11 ? 4 : 0);
    }

    public void turn(FullFto.Move move){
        switch (move){
            case R:
                cycleCorners(2, 1, 4);
                twistCorner(2, 3);
                twistCorner(1, 2);
                twistCorner(4, 3);
                cycleEdges(1, 5, 4);
                cycleThreeCenters(15, 16, 17);
                cycleThreeCenters(3, 1, 8);
                cycleThreeCenters(4, 2, 6);
                break;
            case L:
                cycleCorners(0, 2, 3);
                twistCorner(0, 3);
                twistCorner(2, 2);
                twistCorner(3, 3);
                cycleEdges(2, 3, 8);
                cycleThreeCenters(12, 13, 14);
                cycleThreeCenters(0, 3, 10);
                cycleThreeCenters(2, 5, 9);
                break;
            case U:
                cycleCorners(0, 1, 2);
                cycleEdges(0, 1, 2);
                cycleThreeCenters(0, 1, 2);
                cycleThreeCenters(15, 12, 18);
                cycleThreeCenters(16, 13, 19);
                break;
            case D:
                cycleCorners(3, 4, 5);
                cycleEdges(9, 10, 11);
                cycleThreeCenters(21, 22, 23);
                cycleThreeCenters(5, 8, 11);
                cycleThreeCenters(4, 7, 10);
                break;
            case F:
                cycleCorners(2, 4, 3);
                twistCorner(2, 3);
                twistCorner(4, 3);
                twistCorner(3, 2);
                cycleEdges(4, 9, 3);
                cycleThreeCenters(3, 4, 5);
                cycleThreeCenters(13, 17, 21);
                cycleThreeCenters(15, 22, 14);
                break;
            case B:
                cycleCorners(1, 0, 5);
                twistCorner(1, 3);
                twistCorner(0, 2);
                twistCorner(5, 3);
                cycleEdges(0, 6, 7);
                cycleThreeCenters(18, 19, 20);
                cycleThreeCenters(1, 9, 7);
                cycleThreeCenters(0, 11, 6);
                break;
            case BR:
                cycleCorners(1, 5, 4);
                twistCorner(1, 3);
                twistCorner(5, 3);
                twistCorner(4, 2);
                cycleEdges(5, 7, 10);
                cycleThreeCenters(6, 7, 8);
                cycleThreeCenters(16, 20, 22);
                cycleThreeCenters(17, 18, 23);
                break;
            case BL:
                cycleCorners(0, 3, 5);
                twistCorner(0, 3);
                twistCorner(3, 3);
                twistCorner(5, 2);
                cycleEdges(8, 11, 6);
                cycleThreeCenters(9, 10, 11);
                cycleThreeCenters(19, 14, 23);
                cycleThreeCenters(12, 21, 20);
                break;
            case RP:
                cycleCorners(2, 4, 1);
                twistCorner(2, 2);
                twistCorner(1, 1);
                twistCorner(4, 1);
                cycleEdges(1, 4, 5);
                cycleThreeCenters(15, 17, 16);
                cycleThreeCenters(3, 8, 1);
                cycleThreeCenters(4, 6, 2);
                break;
            case LP:
                cycleCorners(0, 3, 2);
                twistCorner(0, 2);
                twistCorner(2, 1);
                twistCorner(3, 1);
                cycleEdges(2, 8, 3);
                cycleThreeCenters(12, 14, 13);
                cycleThreeCenters(0, 10, 3);
                cycleThreeCenters(2, 9, 5);
                break;
            case UP:
                cycleCorners(0, 2, 1);
                cycleEdges(0, 2, 1);
                cycleThreeCenters(0, 2, 1);
                cycleThreeCenters(15, 18, 12);
                cycleThreeCenters(16, 19, 13);
                break;
            case DP:
                cycleCorners(3, 5, 4);
                cycleEdges(9, 11, 10);
                cycleThreeCenters(21, 23, 22);
                cycleThreeCenters(5, 11, 8);
                cycleThreeCenters(4, 10, 7);
                break;
            case FP:
                cycleCorners(2, 3, 4);
                twistCorner(2, 1);
                twistCorner(4, 2);
                twistCorner(3, 1);
                cycleEdges(4, 3, 9);
                cycleThreeCenters(3, 5, 4);
                cycleThreeCenters(13, 21, 17);
                cycleThreeCenters(15, 14, 22);
                break;
            case BP:
                cycleCorners(1, 5, 0);
                twistCorner(1, 2);
                twistCorner(0, 1);
                twistCorner(5, 1);
                cycleEdges(0, 7, 6);
                cycleThreeCenters(18, 20, 19);
                cycleThreeCenters(1, 7, 9);
                cycleThreeCenters(0, 6, 11);
                break;
            case BRP:
                cycleCorners(1, 4, 5);
                twistCorner(1, 1);
                twistCorner(5, 2);
                twistCorner(4, 1);
                cycleEdges(5, 10, 7);
                cycleThreeCenters(6, 8, 7);
                cycleThreeCenters(16, 22, 20);
                cycleThreeCenters(17, 23, 18);
                break;
            case BLP:
                cycleCorners(0, 5, 3);
                twistCorner(0, 1);
                twistCorner(3, 2);
                twistCorner(5, 1);
                cycleEdges(8, 6, 11);
                cycleThreeCenters(9, 11, 10);
                cycleThreeCenters(19, 23, 14);
                cycleThreeCenters(12, 20, 21);
                break;
        }
    }

    public boolean isSolved(){
        return corners == SOLVED_CORNERS && edges == SOLVED_EDGES && centers == SOLVED_CENTERS;
    }
}
