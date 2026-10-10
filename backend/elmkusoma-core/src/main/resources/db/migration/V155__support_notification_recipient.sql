-- Route contact enquiries to the platform's published contact address.
--
-- Reuses info@elmkusoma.co.tz, which is already the public contact address configured in
-- V152 and already published on the site. This is therefore an address the organisation has
-- chosen, not one invented here. Until now enquiries were stored but nobody was emailed,
-- and visitors correctly saw a "stored but not notified" notice.
--
-- Deliberately NOT seeded into public.site-settings: this is an internal recipient. The key
-- carries a support.* prefix and is never public, so the anonymous settings endpoint cannot
-- disclose where enquiries are routed.
--
-- To change it, edit the value in Platform Admin -> Public site & legal -> Contact & social.
-- The key is kept separate from the public contact address on purpose so support mail can be
-- routed independently of what visitors see. If the published address changes later, the owner
-- should decide here whether support mail should follow it.

UPDATE platform_config
   SET config_value = 'info@elmkusoma.co.tz',
       updated_at   = NOW()
 WHERE config_key = 'support.notify.email'
   AND is_deleted = false;