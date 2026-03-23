package main;
import javax.smartcardio.*;
import java.util.List;

public class HostUtils {

    // AID adapté à celui dans votre MainSimulator.java
    private static final byte[] AID = { 0x5A, 0x43, 0x41, 0x4C, 0x43, 0x59, 0x41, 0x50, 0x50, 0x31 }; 
    
    /**
     * Tente de connecter au premier terminal de carte disponible avec une carte insérée.
     */
    public static CardTerminal connectToCard() throws CardException {
        TerminalFactory factory = TerminalFactory.getDefault();
        List<CardTerminal> terminals = factory.terminals().list();

        if (terminals.isEmpty()) {
            System.err.println(" Aucun lecteur de carte (terminal PC/SC) trouvé.");
            return null;
        }
        
        CardTerminal terminal = terminals.get(0); 
        System.out.println("Terminal sélectionné: " + terminal.getName());
        
        if (!terminal.isCardPresent()) {
            System.err.println(" Aucune carte présente dans le lecteur. Veuillez insérer la carte.");
            return null;
        }

        return terminal;
    }

    /**
     * Envoie la commande SELECT pour l'applet.
     */
    public static void selectApplet(CardChannel channel) throws CardException {
        // Commande SELECT : 00 A4 04 00 [Lc] [AID]
        CommandAPDU select = new CommandAPDU(0x00, 0xA4, 0x04, 0x00, AID);
        ResponseAPDU response = channel.transmit(select);
        
        if (response.getSW() == 0x9000) {
            System.out.println(" Applet sélectionnée (SW: 9000)");
        } else {
            System.err.println(" Échec de la sélection de l'Applet (SW: " + Integer.toHexString(response.getSW()) + ").");
            throw new CardException("Applet selection failed.");
        }
    }
}