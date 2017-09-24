# pgp

### Documentation

* GnuPG [manual](https://www.gnupg.org/gph/en/manual.html)
* MIT pgp [server](http://pgp.mit.edu)

### Setup

```
# create directory to store keys
mkdir -m 700 keys && cd "$_"

# generate keys
gpg2 --homedir . --gen-key

# list keys
gpg2 --homedir . --list-sigs --fingerprint

# generate a revocation certificate
gpg2 --homedir . --output revoke.asc --gen-revoke USER-ID_OR_EMAIL

# export the public key (in ASCII-armored format)
gpg2 --homedir . --export --armor --output pubring.asc

# export the private key
gpg2 --homedir . --export-secret-keys --armor --output secring.asc
```
