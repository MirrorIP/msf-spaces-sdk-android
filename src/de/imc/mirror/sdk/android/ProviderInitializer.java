package de.imc.mirror.sdk.android;

import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.HeaderProvider;
import org.jivesoftware.smackx.provider.HeadersProvider;
import org.jivesoftware.smackx.pubsub.provider.AffiliationProvider;
import org.jivesoftware.smackx.pubsub.provider.AffiliationsProvider;
import org.jivesoftware.smackx.pubsub.provider.ConfigEventProvider;
import org.jivesoftware.smackx.pubsub.provider.EventProvider;
import org.jivesoftware.smackx.pubsub.provider.FormNodeProvider;
import org.jivesoftware.smackx.pubsub.provider.ItemProvider;
import org.jivesoftware.smackx.pubsub.provider.ItemsProvider;
import org.jivesoftware.smackx.pubsub.provider.PubSubProvider;
import org.jivesoftware.smackx.pubsub.provider.RetractEventProvider;
import org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider;
import org.jivesoftware.smackx.pubsub.provider.SubscriptionProvider;
import org.jivesoftware.smackx.pubsub.provider.SubscriptionsProvider;

import de.imc.mirror.sdk.config.NamespaceConfig;
import de.imc.mirror.sdk.android.packet.PersistenceServiceDeleteProvider;
import de.imc.mirror.sdk.android.packet.PersistenceServiceQueryProvider;

/**
 * This class provides a static method to initialize the provider manager.
 * It is used internally by the connection handler.
 * 
 * {@link ConnectionHandler}
 * @author mach
 */
public class ProviderInitializer {
	
	private ProviderInitializer() {}
	
	/**
	 * Adds all IQProvider to the ProviderManager so receiving datapackages will be parsed correctly.
	 */
	public static void initializeProviderManager(){
        /* These rows creates the provider manager that handles different extensions for XMPP
         * basically this tells smack how to handle different XML objects.
         */
        ProviderManager pm = ProviderManager.getInstance();
		pm.addIQProvider("spaces", NamespaceConfig.SPACES_SERVICE, new SpacesProvider());
        pm.addIQProvider("pubsub", NamespaceConfig.XMPP_PUBSUB, new PubSubProvider());
        pm.addIQProvider("pubsub", NamespaceConfig.XMPP_PUBSUB + "#owner", new PubSubProvider());
        pm.addExtensionProvider("subscription", NamespaceConfig.XMPP_PUBSUB, new SubscriptionProvider());
        pm.addExtensionProvider("subscriptions", NamespaceConfig.XMPP_PUBSUB, new SubscriptionsProvider());
        pm.addExtensionProvider("subscriptions", NamespaceConfig.XMPP_PUBSUB + "#owner", new SubscriptionsProvider());
        pm.addExtensionProvider("affiliations", NamespaceConfig.XMPP_PUBSUB, new AffiliationsProvider());
        pm.addExtensionProvider("affiliation", NamespaceConfig.XMPP_PUBSUB, new AffiliationProvider());
        pm.addExtensionProvider("options", NamespaceConfig.XMPP_PUBSUB, new FormNodeProvider());
        pm.addExtensionProvider("options", NamespaceConfig.XMPP_PUBSUB + "#event", new FormNodeProvider());
        pm.addExtensionProvider("configure", NamespaceConfig.XMPP_PUBSUB + "#owner", new FormNodeProvider());
        pm.addExtensionProvider("default", NamespaceConfig.XMPP_PUBSUB + "#owner", new FormNodeProvider());
        pm.addExtensionProvider("event", NamespaceConfig.XMPP_PUBSUB + "#event", new EventProvider());
        pm.addExtensionProvider("configuration", NamespaceConfig.XMPP_PUBSUB + "#event", new ConfigEventProvider());
        pm.addExtensionProvider("delete", NamespaceConfig.XMPP_PUBSUB + "#event", new SimpleNodeProvider());
        pm.addExtensionProvider("create", NamespaceConfig.XMPP_PUBSUB, new SimpleNodeProvider());
        pm.addExtensionProvider("retract", NamespaceConfig.XMPP_PUBSUB + "#event", new RetractEventProvider());
        pm.addExtensionProvider("purge", NamespaceConfig.XMPP_PUBSUB + "#event", new SimpleNodeProvider());
        pm.addExtensionProvider("items", NamespaceConfig.XMPP_PUBSUB, new ItemsProvider());
        pm.addExtensionProvider("items", NamespaceConfig.XMPP_PUBSUB + "#event", new ItemsProvider());
        pm.addExtensionProvider("item", NamespaceConfig.XMPP_PUBSUB, new ItemProvider());
        pm.addExtensionProvider("item", NamespaceConfig.XMPP_PUBSUB + "#event", new ItemProvider());
        pm.addExtensionProvider("item", "", new ItemProvider());
        
        pm.addExtensionProvider("headers", "http://jabber.org/protocol/shim", new HeadersProvider());
        pm.addExtensionProvider("header", "http://jabber.org/protocol/shim", new HeaderProvider());
        
        pm.addExtensionProvider("x", "jabber:x:data", new DataFormProvider());
        
        pm.addIQProvider("query", "http://jabber.org/protocol/disco#items", new DiscoverItemsProvider());
        pm.addIQProvider("query", "http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());
        pm.addIQProvider("query", NamespaceConfig.PERSISTENCE_SERVICE, new PersistenceServiceQueryProvider());
        pm.addIQProvider("delete", NamespaceConfig.PERSISTENCE_SERVICE, new PersistenceServiceDeleteProvider());
	}
}
