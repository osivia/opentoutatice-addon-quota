# opentoutatice-addon-quota

### Personnalisation du message d'erreur associé à l'exception QuotaExceededException:
Fichier /opentoutatice-addon-quota/src/main/resources/OSGI-INF/l10n/messages_fr.properties: clé label.error.quota.exceeded

<b>Note:</b> Si vous souhaitez un message avec paramètres, il faut valoriser le 3ème paramètre du constructeur de l'exception (type String[]) dans la classe QuotaChecker (l.76)