-- Official social links supplied by the platform owner.
--
-- Stored verbatim. Neither value is normalised, shortened or "cleaned up":
--   facebook  - https://www.facebook.com/share/1Exp9TeDmL/
--                A /share/ link, NOT a permanent Page URL. It could not be verified
--                from the deployment environment (Facebook rejects non-interactive
--                requests), so it is retained exactly as supplied and flagged for the
--                owner to replace with the canonical Page URL when available.
--   instagram - https://www.instagram.com/elmkusoma_20?srtk=bnFrZG00NmpzcG8=
--                Verified reachable (HTTP 200), and the profile path resolves on its
--                own. The `srtk` value is a transient share/referral token: harmless,
--                but the owner may prefer the bare profile URL for a permanent link.
--
-- No other social platform is touched. YouTube, LinkedIn, TikTok and X stay present
-- and editable; they remain empty because no official URL has been supplied for them,
-- and the public footer hides them until configured rather than inventing links.

UPDATE platform_config
   SET config_value = 'https://www.facebook.com/share/1Exp9TeDmL/',
       updated_at   = NOW()
 WHERE config_key = 'public.social.facebook'
   AND is_deleted = false;

UPDATE platform_config
   SET config_value = 'https://www.instagram.com/elmkusoma_20?srtk=bnFrZG00NmpzcG8=',
       updated_at   = NOW()
 WHERE config_key = 'public.social.instagram'
   AND is_deleted = false;