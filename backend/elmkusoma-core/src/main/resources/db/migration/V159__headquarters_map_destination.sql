-- Owner-supplied map destination for the ELMKUSOMA Headquarters.
--
-- https://share.google/3bCJMBlMZaSEvTIG5
--
-- Stored verbatim as public.location.mapUrl. No parameters are appended, no coordinates are
-- added, and no "directions" variant is invented: a share.google link is an opaque short link,
-- and fabricating a turn-by-turn URL from it would be a guess presented as a fact.
--
-- WHAT THE LINK ACTUALLY RESOLVES TO (verified 2026-10-11)
--
-- Following the redirect chain lands on a Google Maps knowledge-graph entry with
-- kgmid=/g/1hc452kzg and query "University of Dar Salaam Business School" — i.e. the University
-- of Dar es Salaam Business School (UDBS), a constituent college of UDSM. That is a UDSM
-- location, and more precise than campus level, so the map action is now enabled.
--
-- It is still not an office pin: a knowledge-graph place is a campus/building-level record, and
-- nothing in it identifies an ELMKUSOMA office, room or entrance. That is why
-- public.location.building and public.location.address remain empty, and why
-- public.location.directionsUrl is left unset — turn-by-turn directions need a precise
-- destination, and enabling them here would send a visitor to the campus with no way to find us.
--
-- The displayed location text (name/campus/city/country) is deliberately NOT changed by this
-- migration. The owner stated the headquarters is at the University of Dar es Salaam (UDSM);
-- rewriting that wording to name a specific college is the owner's call, not a migration's.

UPDATE platform_config
   SET config_value = 'https://share.google/3bCJMBlMZaSEvTIG5',
       updated_at   = NOW()
 WHERE config_key = 'public.location.mapUrl'
   AND is_deleted = false;
