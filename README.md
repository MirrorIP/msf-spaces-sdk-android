# MIRROR Spaces SDK for Android
This SDK provides high-level interfaces for the services of the [MIRROR Spaces Framework (MSF)][1]. It allows developers without deeper knowledge of the XMPP protocol to connect their android applications to the MSF with only a few lines of code.

The JARs are compiled for Android Level 8, i.e. Android 2.2 and newer.

The implementation is build on top of the [ASmack XMPP library][2].

## Build and Deploy
In order to run a build, ensure to update the path to the `android.jar` file in `build.properties`. Then run the build with Apache Ant:

    $ ant build

To integrate the SDK in your application, add the following JARs provided in the "dist" directory :

    jdom-2.0.5.jar
    asmack-android-8-0.8.10.jar
    spaces-sdk-api-1.3.jar
    spaces-sdk-android-1.3.0.jar

You can also use the `DEBUG` versions of the SDK libraries in order to have a full API documentation integrated.

## Usage
The SDK for Java implements the [Space SDK API][3]. Three major handlers are provided to use the MSF:

1. **[ConnectionHandler][4]**
  A handler to establish and manage the connection to the XMPP server.
2. **[SpaceHandler][5]**
  This handler provides a set of methods to manage the spaces you can access.
  There are methods for the access, creation, management, and deletion of
  spaces. For space management operations the application has to be online.
  Retrieving space information is also possible in offline mode, as long as the
  information is available in the handler's cache. 
3. **[DataHandler][6]**
  The data handler provides methods to register for notifications about newly
  published data, and with methods to send and retrieve it. All items published
  and received while being online are stored and therefore also available when
  the handler is offline. Additionally, the allows queries to the [persistence service][7].

Details about the usage of the handlers are available of the SDK API documentation.

Guides how to use the SDK are available on GitHub:
https://github.com/MirrorIP/msf-spaces-sdk-android/wiki

The complete API documentation for the Java SDK is available here:
http://docs.mirror-demo.eu/spaces-sdk/android/1.3.0/

The general API description is also available:
http://docs.mirror-demo.eu/spaces-sdk/api/1.3/

## License
The Spaces SDK for Java is provided under the [Apache License 2.0][8].
License information for third party libraries is provided with the related JAR files.

## Changelog

v1.3.0 - April 3, 2014

* [NEW] Implements Spaces SDK API 1.3.
* [NEW] Full support for CDM 2.0.
* [NEW] Added experimental (non-API) call for synchronous publishing of data objects: `DataHandler.publishAndRetrieveDataObject()`.
* [FIX] Fixed usage of XML namespaces.
* [FIX] Fixed a bug in `DataObjectBuilder` that prevented added elements to be parsed correctly.
* [UPDATE] Updated to ASmack 0.8.10.
* [UPDATE] Updated to JDOM 2.0.5.
 
v1.2.3 - June 11, 2013

* [FIX] Synchronized sqlite database access.

v1.2.2 - May 14, 2013

* [FIX] The Itemprovider now parses items correctly.

v1.2.1 - April 19, 2013

* [FIX] The data handler now handles data objects without identifier correctly.
* [FIX] OrgaSpace: The list of supported data models is now returned correctly. 

v1.2 - April 15, 2013

* [FIX] DataObjectBuilder: Text content of a data object child element no longer requires to be valid XML.
* [FIX] Removed unnecessary requests when publishing data objects.
* [UPDATE] Accepts Interop Data Models as MIRROR Data Models.
* [NEW] Model classes (CDMData*, DataModel, DataObject, *Space, SpaceChannel, SpaceMember) are now serializable.
* [NEW] Added compatibility with MIRROR Persistence Service.
* [NEW] Removed dependency for Simple XML framework. 

v1.1 - November 8, 2012

* Implements Spaces SDK API 1.1.
* Added XMPP connection handler.
* Several updates to existing handlers.

v1.0 - October 26, 2012

* First release.
* Compatible with MIRROR Spaces Service 0.4.x.

  [1]: https://github.com/MirrorIP
  [2]: https://github.com/flowdalic/asmack
  [3]: https://github.com/MirrorIP/msf-spaces-sdk-api
  [4]: http://docs.mirror-demo.eu/spaces-sdk/android/1.3.0/index.html?de/imc/mirror/sdk/android/ConnectionHandler.html
  [5]: http://docs.mirror-demo.eu/spaces-sdk/android/1.3.0/index.html?de/imc/mirror/sdk/android/SpaceHandler.html
  [6]: http://docs.mirror-demo.eu/spaces-sdk/android/1.3.0/index.html?de/imc/mirror/sdk/android/DataHandler.html
  [7]: https://github.com/MirrorIP/msf-persistence-service
  [8]: http://www.apache.org/licenses/LICENSE-2.0.html
