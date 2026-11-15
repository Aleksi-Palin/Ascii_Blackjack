# ASCII Blackjack

A blackjack game that runs in the terminal. It includes ASCII playing cards,
chip betting, doubling down, blackjack payouts, and optional animations.

## What it looks like
<img width="346" height="353" alt="image" src="https://github.com/user-attachments/assets/43a70cd8-5a07-41f7-8694-99e9594a44b4" />


<img width="577" height="259" alt="image" src="https://github.com/user-attachments/assets/0e4964fb-4471-409c-a442-9365579850a1" />


## Requirements

- Java 17 or newer
- PowerShell or another terminal
- Maven is optional

Check that Java is installed:

```powershell
java -version
javac -version
```

## Run without Maven

Open PowerShell in the project folder and compile the game:

```powershell
New-Item -ItemType Directory -Force target\classes
javac --release 17 -d target\classes src\main\java\blackjack\*.java
```

Start the game with animations:

```powershell
java -cp "target\classes;src\main\resources" blackjack.Game --anim
```

To disable animations, use `--no-anim` instead:

```powershell
java -cp "target\classes;src\main\resources" blackjack.Game --no-anim
```

### Game options

- `--anim` forces animations on.
- `--no-anim` turns animations off.
- Without either option, animations are enabled automatically when the
  terminal supports them.
- Enter `q` when prompted for a bet to quit.

## Run with Maven

Maven must be installed separately from the VS Code Maven extension. From the
project folder, run:

```powershell
mvn compile exec:java -Dexec.args="--anim"
```

To run without animations:

```powershell
mvn compile exec:java -Dexec.args="--no-anim"
```

## Run the checks

Without Maven:

```powershell
New-Item -ItemType Directory -Force target\classes,target\test-classes
javac --release 17 -d target\classes src\main\java\blackjack\*.java
javac --release 17 -cp target\classes -d target\test-classes src\test\java\blackjack\*.java
java -cp "target\test-classes;target\classes;src\main\resources" blackjack.CheckRules
```

With Maven:

```powershell
mvn test-compile
java -cp "target\test-classes;target\classes" blackjack.CheckRules
```

## Project layout

```text
src/main/java/blackjack/       Game source code
src/main/resources/anim/       Animation files
src/test/java/blackjack/       Checks
```
