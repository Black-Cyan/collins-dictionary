# Arch Linux Installation

The AUR package `collins-dictionary-bin` is not yet available on the AUR
because new account registration is currently closed. In the meantime you
can build and install the package locally.

## Install

```bash
git clone https://github.com/Black-Cyan/collins-dictionary.git
cd collins-dictionary/packaging/aur

# Download the release archive and update checksums
updpkgsums

# Build and install
makepkg -si
```

After installation the application is available as `collins-dictionary`
in your PATH, and a desktop entry is added to your application launcher.

## Uninstall

```bash
sudo pacman -Rns collins-dictionary-bin
```
