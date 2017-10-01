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
>>> add local.* to .gitignore

mkdir .travis
cp keys/pubring.asc keys/secring.asc .travis
mv .travis/pubring.asc .travis/local.pubring.asc
mv .travis/secring.asc .travis/local.secring.asc

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

# encrypt Bintray credentials (username/API Key)(escape special characters)
travis encrypt 'BINTRAY_USER=XXX' --add
travis encrypt 'BINTRAY_PASS=XXX' --add

>>> @see after_success
>>> @see deploy

>>> add new Maven package on Bintray
```

## coveralls

* [sbt-coveralls](https://github.com/scoverage/sbt-coveralls)

```
# login with github credentials
travis login

# encrypt Token
travis encrypt 'COVERALLS_REPO_TOKEN=XXX' --add
```

## scaladoc on GitHub

* [sbt-site](http://www.scala-sbt.org/sbt-site/publishing.html)

```
# init gh-pages branch
sbt clean makeSite
origin=$(git remote get-url origin)
cd lib/target/site
git init
git add .
git commit -m "Initial import of GitHub Pages"
git push --force "$origin" master:gh-pages

# publish gh-pages
sbt ghpagesPushSite
# cache issue
# [error] fatal: Not a git repository (or any of the parent directories): .git
rm -r ~/.sbt/ghpages/

# add index.html with redirect
touch lib/target/site/index.html
 
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Project Documentation</title>
    <script language="JavaScript">
        <!--
        function doRedirect()
        {
            window.location.replace("latest/api/com/github/niqdev/stream/index.html");
        }

        doRedirect();
        //-->
    </script>
</head>
<body>
<a href="latest/api">Go to the project documentation
</a>
</body>
</html>

sbt ghpagesPushSite
```
