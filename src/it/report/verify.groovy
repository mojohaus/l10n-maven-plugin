// report file exist
def report = new File( basedir, "target/site/l10n-status.html" )
assert report.exists()

def reportBody = report.text

// simple assertion for content in report
assert reportBody.contains( 'L10n Status' )
assert reportBody.contains( 'test.properties' )
assert reportBody.contains( 'en - English' )
assert reportBody.contains( 'de - German' )
