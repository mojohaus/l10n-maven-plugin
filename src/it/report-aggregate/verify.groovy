// report file exist
def report = new File( basedir, "target/site/l10n-status.html" )
assert report.exists()

def reportBody = report.text

// simple assertion for content in report
assert reportBody.contains( 'L10n Status' )
assert reportBody.contains( 'test.properties' )
assert reportBody.contains( 'en - English' )
assert reportBody.contains( 'de - German' )
assert reportBody.contains( 'mod1' )
assert reportBody.contains( 'mod2' )


// report files not exist in modules

def reportMod1Site = new File( basedir, "mod1/target/site" )
def reportMod1 = new File( reportMod1Site, "l10n-status.html" )

assert reportMod1Site.isDirectory()
assert !reportMod1.exists()


def reportMod2Site = new File( basedir, "mod2/target/site" )
def reportMod2 = new File( reportMod2Site, "l10n-status.html" )

assert reportMod2Site.isDirectory()
assert !reportMod2.exists()
