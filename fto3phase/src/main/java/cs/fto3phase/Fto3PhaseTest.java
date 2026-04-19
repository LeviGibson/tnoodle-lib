package cs.fto3phase;

public class Fto3PhaseTest {


    public static void testTurn(){
        FullFto fto = new FullFto();
        fto.parseAlg("D' R D' B' D B D U' R U' L B U' L U R U B R' L' BL D' L' U B' BL BL' B U' L D BL'  L R B' U' R' U' L' U B' L' U R' U D' B' D' B D R' D");
        assert (fto.isSolved());
    }

    public static void main(String[] args){
        testTurn();
    }
}
