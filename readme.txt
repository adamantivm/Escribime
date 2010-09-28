Sept 27, 2010:
Searched for an API to interface with the GMail app in Android: no luck

Found this GMail REST API: http://code.google.com/apis/gmail/docs/inbox_feed.html

Example below:

julian@betun:~$ curl -u jcerruti https://mail.google.com/mail/feed/atom
Enter host password for user 'jcerruti':
<?xml version="1.0" encoding="UTF-8"?>
<feed version="0.3" xmlns="http://purl.org/atom/ns#">
<title>Gmail - Inbox for jcerruti@gmail.com</title>
<tagline>New messages in your Gmail Inbox</tagline>
<fullcount>0</fullcount>
<link rel="alternate" href="http://mail.google.com/mail" type="text/html" />
<modified>2010-09-28T01:25:08Z</modified>
</feed>
julian@betun:~$ curl -u jcerruti https://mail.google.com/mail/feed/atom/other
Enter host password for user 'jcerruti':
<?xml version="1.0" encoding="UTF-8"?>
<feed version="0.3" xmlns="http://purl.org/atom/ns#">
<title>Gmail - Label &#39;other&#39; for jcerruti@gmail.com</title>
<tagline>New messages in your &#39;other&#39; label</tagline>
<fullcount>0</fullcount>
<link rel="alternate" href="http://mail.google.com/mail" type="text/html" />
<modified>2010-09-28T01:25:23Z</modified>
</feed>
