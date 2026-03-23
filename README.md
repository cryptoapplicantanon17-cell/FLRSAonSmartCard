# FLRSA-JavaCard (J3R180 Implementation)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![JavaCard](https://img.shields.io/badge/Platform-JavaCard%203.0.5-blue.svg)](https://www.oracle.com/java/technologies/javacard-sdk-downloads.html)

This repository provides a high-performance implementation of the **FLRSA Signature** algorithm specifically optimized for the **NXP JCOP J3R180** smart card.

---

## 🛡️ Security Expertise & Heritage

This implementation focuses on embedding advanced cryptanalysis countermeasures into hardware:

### 1. Resistance to Small Private Exponents ($d_p$, $d_q$)
Following the security recommendations of **David Vigilant** (*"RSA with Unknown Public Exponent"*, CT-RSA 2008), this applet is designed for environments where the **Public Key is kept unknown**. This significantly hardens the system against lattice-based attacks on embedded devices.

### 2. Protection against Chosen Plaintext Attacks (CPA)
As analyzed by **Burt Kaliski**, deterministic schemes are vulnerable. We recommend using this implementation with **PSS** or **PKCS #1 v1.5** padding to introduce randomness (salts) unknown to the attacker, ensuring that identical messages produce different signatures.

---

## 🚀 Technical Features

- **Algorithm:** FLRSA Signature (1024-bit).
- **Optimization:** Uses the **Cubic Expansion formula** $(c^3 - c)$ which eliminates the need for modular inversion of 2 ($inv2$).
- **Efficiency:** Reduced computational footprint, ideal for lightweight secure elements.
- **Hardware Target:** NXP JCOP J3R180 (JCOP 4).



---
## 🛠️ Requirements & Dependencies

To build and deploy this applet, ensure your environment meets the following specifications:

### 1. Development Environment
* **Java JDK:** Version **17** (Required for Gradle 8.7 compatibility).
* **Build Tool:** **Gradle 8.7** (Managed via the included `gradlew` wrapper).
* **JavaCard SDK:** Version **2.2.1** (Target classic edition for J3R180 backward compatibility).

### 2. Libraries
* **JCMathLib:** This project depends on the [JCMathLib](https://github.com/OpenCryptoProject/JCMathLib) library for modular arithmetic and big integer support on JavaCard. 
    * *Note: Ensure the JCMathLib folder is present in the project root or linked as a git submodule.*

### 3. Hardware Target
* **Smart Card:** NXP JCOP J3R180 (JCOP 4).
* **Connectivity:** PC/SC compliant card reader.


## 🛠️ Compilation and Build

This project uses the **Gradle wrapper** to ensure a consistent build environment without requiring a manual Gradle installation.

### How to generate the CAP file

To build the project and generate the binary for your smart card, run the following command in your terminal:

./gradlew buildJavaCard

### Build Instructions
1. **Clone the repository** (including submodules):
  git clone --recursive  https://github.com/cryptoapplicantanon17-cell/FLRSAonSmartCard.git

