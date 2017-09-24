# How to

## pgp

### Documentation

* GnuPG [manual](https://www.gnupg.org/gph/en/manual.html)
* MIT pgp [server](http://pgp.mit.edu)
* Config [sbt-release-early](https://github.com/scalacenter/sbt-release-early/wiki/How-to-create-a-gpg-key)

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

## Travis CI

### Documentation

* Build [Scala](https://docs.travis-ci.com/user/languages/scala) project
* Setup Travis [cli](https://github.com/travis-ci/travis.rb#installation)
* Config [sbt-release-early](https://github.com/scalacenter/sbt-release-early/wiki/How-to-release-in-Travis-(CI))

```
# install travis cli
sudo gem2.3 install travis -v 1.8.8 --no-rdoc --no-ri

# login with github credentials
travis login

# tar-gzip the files to be encoded
tar cv -C .travis -f .travis/local.secrets.tar local.pubring.asc local.secring.asc

# encode the secrets
travis encrypt-file .travis/local.secrets.tar -o .travis/secrets.tar.enc -p

>>> @see before_install

# encrypt the PGP passphrase
travis encrypt 'PGP_PASS=XXX' --add

# encrypt Bintray credentials
travis encrypt 'BINTRAY_USER=XXX' --add
travis encrypt 'BINTRAY_PASS=XXX' --add

>>> @see after_success
>>> @see deploy
```
