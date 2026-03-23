/**
 * Project: FLRSA-JavaCard
 * Implementation of Fast and Lightweight RSA Signature (1024-bit)
 * Target Hardware: NXP JCOP J3R180
 * * Credits:
  * - Optimized with Cubic Expansion formula (c^3 - c).
 * * Dependencies:
 * - JCMathLib (https://github.com/OpenCryptoProject/JCMathLib)
 * * License: MIT License
 * Copyright (c) 2026 Anon22
 * * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * * This project acknowledges the use of JCMathLib under MIT License.
 */



package opencrypto.jcmathlib;

import javacard.framework.*;
import opencrypto.jcmathlib.BigNat;
import opencrypto.jcmathlib.ResourceManager;


import javacard.security.KeyBuilder;
import javacard.security.RSAPrivateKey;
import javacardx.crypto.Cipher;



public class ComplexCalcApplet extends Applet {
    private RSAPrivateKey rsaKey;
    private Cipher rsaCipher;

    // Commandes
    // Commands
    final static byte CLA_INIT = (byte) 0xB0;
    final static byte INS_INIT = (byte) 0x10;
    final static byte CLA_CALC = (byte) 0x80;
    final static byte INS_DO_CALC = (byte) 0x03;

    // --- Taille de clé réduite à 64 bytes (512 bits) ---
    // --- Key size reduced to 128 bytes ( 1024 bits) ---
    private final static short KEY_SIZE_BYTES = 128; 
    private final static short DELTA_SIZE_BYTES = 128;
    private BigNat EXP_TWO; // Exposant constant = 2
    private BigNat EXP_THREE;
    // --- OBJETS PERSISTANTS (EEPROM) ---
    // --- PERSISTENT OBJECTS (EEPROM) ---
    private BigNat n;
    private BigNat coeff2;
    private BigNat inv6;
    private BigNat delta;
    private BigNat T; // Accumulateur et résultat final (PERSISTANT)
    
    // --- OBJETS TRANSIENTS (RAM) ---
    // --- TRANSIENT OBJECTS (RAM) ---
    private BigNat x; // Input/output 
    

    private ResourceManager rm;

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new ComplexCalcApplet().allocateAndRegister();
    }

    private ComplexCalcApplet() {}

    private void allocateAndRegister() {
        // GARDER 1024 POUR L'INSTALLATION (contrainte physique)
        // KEEP 1024 FOR INSTALLATION (physical constraint)
        rm = new ResourceManager(JCSystem.MEMORY_TYPE_PERSISTENT, (short) 1024); 

        // Allocations PERSISTANTES (EEPROM)
        // PERSISTENT allocations (EEPROM)
        n      = new BigNat(KEY_SIZE_BYTES, (byte) 0, rm);
       coeff2 = new BigNat(KEY_SIZE_BYTES, (byte) 0, rm);
        inv6   = new BigNat(KEY_SIZE_BYTES, (byte) 0, rm);
        delta  = new BigNat(DELTA_SIZE_BYTES, (byte) 0, rm);
        T      = new BigNat(KEY_SIZE_BYTES, (byte) 0, rm);

        // Allocations TRANSIENTES (RAM)
        // TRANSIENT allocations (RAM)
        x = new BigNat(KEY_SIZE_BYTES, JCSystem.MEMORY_TYPE_TRANSIENT_RESET, rm);
        EXP_TWO = new BigNat((short) 1, (byte) 0, rm); 
        byte[] val = {0x02};
        EXP_TWO.fromByteArray(val, (short) 0, (short) 1);
        EXP_THREE = new BigNat((short) 1, (byte) 0, rm); 
        byte[] val2 = {0x03};
        EXP_THREE.fromByteArray(val2, (short) 0, (short) 1);
        // ALLOCATION RSA POUR ACCÉLÉRATION modExp
        // RSA ALLOCATION FOR modExp ACCELERATION
        rsaKey = (RSAPrivateKey) KeyBuilder.buildKey(
        KeyBuilder.TYPE_RSA_PRIVATE,
        (short)(KEY_SIZE_BYTES * 8), // Taille en bits (ex: 1024)
        false
        );
        rsaCipher = Cipher.getInstance(
        Cipher.ALG_RSA_NOPAD, 
        false
    );

        register();
    }
    /**
 * Exponentiation modulaire (base^exp mod mod) utilisant l'accélération RSA.
 */
 /**
 * Modular exponentiation (base^exp mod mod) using RSA acceleration.
 */
private void modExp_Accelerated(BigNat base, BigNat exp, BigNat mod, BigNat result) {
    
    // 1. Charger le module (n) et l'exposant (delta) dans la clé RSA
    // 1. Load the modulus (n) and the exponent (delta) into the RSA key
    short size = mod.length();
    byte[] tmp = new byte[size];
    short sizeexp = exp.length();
    byte[] tmpexp = new byte[sizeexp];

    // Copier la valeur du BigNat 'mod' dans tmp
    // Copy the BigNat 'mod' value into tmp
    mod.copyToByteArray(tmp, (short) 0);
    exp.copyToByteArray(tmpexp, (short) 0);

    // Charger dans la clé RSA
    // Load into the RSA key
    rsaKey.setModulus(tmp, (short) 0, size);
    rsaKey.setExponent(tmpexp, (short) 0, sizeexp);
    
    // 2. Initialiser le Cipher en mode DÉCHIFFREMENT
    // 2. Initialize the Cipher in DECRYPT mode
 
    rsaCipher.init(rsaKey, Cipher.MODE_DECRYPT);

    // 3. Exécuter l'opération (le déchiffrement RSA est base^exp mod mod)
    // 3. Execute the operation (RSA decryption is base^exp mod mod)

    
    short modSize = mod.length();
    byte[] alignedBuf = new byte[modSize];
    base.copyToByteArray(alignedBuf, (short) 0);
    short outputLen = rsaCipher.doFinal(
            alignedBuf, (short) 0, base.length(), 
            alignedBuf, (short) 0
        );
        
        // CORRECTION CRITIQUE (FIX ZÉRO) : Alignement du résultat dans alignedBuf
       // CRITICAL FIX (ZERO PADDING): Aligning the result in alignedBuf
        short offset = (short) (modSize - outputLen);
        
       
        Util.arrayCopy(alignedBuf, (short) 0, alignedBuf, offset, outputLen);
        
       
        Util.arrayFill(alignedBuf, (short) 0, offset, (byte) 0x00);
        
       
        result.fromByteArray(alignedBuf, (short) 0, modSize); 
        result.setSize(modSize); // Version 4 arguments si disponible
        rm.refreshAfterReset();
        
        tmp = null;
        tmpexp = null;
        alignedBuf = null;


}



    public void process(APDU apdu) {
        if (selectingApplet()) return;

        byte[] buf = apdu.getBuffer();
        byte cla = buf[ISO7816.OFFSET_CLA];
        byte ins = buf[ISO7816.OFFSET_INS];

        // Gestion des CLA sécurisés
        // Secure CLA handling
        if ((cla == CLA_INIT || cla == (byte)(CLA_INIT | 0x04)) && ins == INS_INIT) {
            initConstants(apdu);
            return;
        }

        if ((cla == CLA_CALC || cla == (byte)(CLA_CALC | 0x04)) && ins == INS_DO_CALC) {
            doCalc(apdu);
            return;
        }

        ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
    }

    private void initConstants(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        apdu.setIncomingAndReceive();
        short off = ISO7816.OFFSET_CDATA;

        switch (buf[ISO7816.OFFSET_P1]) {
            case 0x01: n.fromByteArray(buf, off, KEY_SIZE_BYTES); break;
            case 0x02: coeff2.fromByteArray(buf, off, KEY_SIZE_BYTES); break;
            case 0x03: inv6.fromByteArray(buf, off, KEY_SIZE_BYTES); break;
            case 0x04: delta.fromByteArray(buf, off, DELTA_SIZE_BYTES); break;
            default: ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
        }
    }

    private void doCalc(APDU apdu) {

        byte[] buf = apdu.getBuffer();
        apdu.setIncomingAndReceive();

        x.fromByteArray(buf, ISO7816.OFFSET_CDATA, KEY_SIZE_BYTES); // x est transient

        // 1. Calcul de la Partie Gauche : T = (((x^3 - x) * coeff2 * inv6) + x) mod n
        // 1. Left-Hand Side Calculation: T = (((x^3 - x) * coeff2 * inv6) + x) mod n
        
        T.copy(x); 
        //x3
        modExp_Accelerated(x, EXP_THREE, n, T);
        // x³ - x
        T.modSub(x, n);
        T.modMult(coeff2, n);
        // * coeff2
        //modMult_Accelerated(T, coeff2, n, T);
        // * inv6
        T.modMult(inv6, n);
        //modMult_Accelerated(T, inv6, n, T);
        // + x
        T.modAdd(x, n); 

        // 2. Calcul de la Partie Droite : x = x^Δ (RÉACTIVÉ)
        // 2. Right-Hand Side Calculation: x = x^Δ (REACTIVATED)
        //x.modExp(delta, n);
        modExp_Accelerated(x, delta, n,x);
        // modExp_Accelerated(x, EXP_TWO, n,x);
          //modExp_Accelerated(x, EXP_TWO, n,x);
           // modExp_Accelerated(x, EXP_TWO, n,x);
        //T.copy(x);

        // 3. Calcul Final: T = T * x^Δ
        // 3. Final Calculation: T = T * x^Δ
        //modMult_Accelerated(T, x, n, T);
        T.modMult(x,n);
        //T.copy(x);
 

        short L = T.length();
        apdu.setOutgoing();
        apdu.setOutgoingLength(L);
        T.copyToByteArray(buf, (short) 0);
        apdu.sendBytesLong(buf, (short) 0, L);
        
        rm.refreshAfterReset();
    }
}
