import urllib.request
import xml.etree.ElementTree as ET

def define_env(env):
    """
    This is the hook for defining variables, macros and filters
    """
    @env.macro
    def library_version():
        url = "https://repo1.maven.org/maven2/io/github/dp-hridayan/lazyselectioncontainer/maven-metadata.xml"
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(req, timeout=5) as response:
                xml_data = response.read()
                root = ET.fromstring(xml_data)
                
                # The latest stable release is usually under versioning -> release
                release = root.find('.//versioning/release')
                if release is not None and release.text:
                    return release.text
                
                # Fallback to the last version in the list if <release> is missing
                versions = root.findall('.//versions/version')
                if versions:
                    return versions[-1].text
        except Exception as e:
            print("Failed to fetch version from Maven Central:", e)
            pass
            
        return "1.5.0" # Fallback just in case network is down
