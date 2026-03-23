package opencrypto.jcmathlib;

import java.math.BigInteger;
import javax.smartcardio.*;
import java.security.SecureRandom;
import static main.HostUtils.*;
 

public class CalculateHost {

    // --- Constantes APDU pour le Calcul (adaptées de MainSimulator.java) ---
    private static final byte CLA_CALC = (byte) 0x80;
    private static final byte INS_DO_CALC = (byte) 0x03;
    private static final int KEY_SIZE_BYTES = 128; // 128 bytes

    public static void main(String[] args) {
        try {
            CardTerminal terminal = HostUtils.connectToCard();
            if (terminal == null) return;
            
            Card card = terminal.connect("*");
            CardChannel channel = card.getBasicChannel();
            System.out.println("Connexion à la carte établie pour le CALCUL.");

            HostUtils.selectApplet(channel);
            
            // --- Phase de Calcul ---
            System.out.println("\nDéclenchement du Calcul Complexe (CLA=80, INS=03) ---");
            
            // 1. Générer une entité x aléatoire de 128 bytes
            byte[] x_bytes = new byte[KEY_SIZE_BYTES]; 
            new SecureRandom().nextBytes(x_bytes);
            System.out.println("Entité x (" + KEY_SIZE_BYTES + " bytes) générée aléatoirement.");

            // 2. Exécuter le calcul
            doComplexCalculation(channel, x_bytes); 

            card.disconnect(false);
            System.out.println("\nDéconnexion de la carte.");
            
        } catch (Exception e) {
            System.err.println("\nErreur critique lors du calcul : " + e.getMessage());
        }
    }

    /**
     * Déclenche l'APDU INS_DO_CALC avec l'entité x en DATA.
     */
    private static void doComplexCalculation(CardChannel channel, byte[] x_data) throws CardException {
        
        // La méthode CommandAPDU(CLA, INS, P1, P2, data) est l'équivalent correct 
        // dans javax.smartcardio de l'encodage [80 03 00 00 80 DATA].
        CommandAPDU calcApdu = new CommandAPDU(CLA_CALC, INS_DO_CALC, 0x00, 0x00, x_data);
        ResponseAPDU response = channel.transmit(calcApdu);

        if (response.getSW() != 0x9000) {
            System.err.println("Échec de l'exécution du calcul (SW: " + Integer.toHexString(response.getSW()) + ")");
            return;
        }

        // Récupérer et afficher le résultat
        byte[] resultBytes = response.getData();
        BigInteger resultOnCard = new BigInteger(1, resultBytes);
        System.out.println("\nRÉSULTAT REÇU ---");
        System.out.println("Longueur : " + resultBytes.length + " bytes");
        // Afficher seulement le début pour lisibilité
        String hexResult = resultOnCard.toString(16);
        System.out.println("Début du résultat (hex) : " + (hexResult.length() > 40 ? hexResult.substring(0, 40) + "..." : hexResult));
        System.out.println("Succès (SW: 9000).");
    }
}