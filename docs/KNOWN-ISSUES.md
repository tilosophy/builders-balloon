# Known issues and beta feedback

## A Good Place placement-animation pack

During development, the tested A Good Place placement-animation resource pack could show misplaced balloons and enlarged/floating anchors during rapid placement. The issue occurred with shaders enabled and disabled. A clean instance containing only Builder's Balloon did not reproduce it; disabling the affected animation pack resolved the observed problem.

**Workaround:** disable that placement-animation resource pack. A Good Place itself did not have to be removed in the tested setup.

The exact pack name/version was not recorded, the mechanism is unconfirmed, and this is not a claim about every A Good Place configuration. No gameplay/data damage was reported. A fix is deferred.

## Beta testing boundary

Real dedicated multiplayer gameplay testing is deferred. Development included packaged server automation and owner gameplay/compatibility checks, but that does not establish all interactions between real players. Later NeoForge 21.1 releases are allowed by metadata; the tested launch baseline is 21.1.252. Sable support is not a general guarantee for all ship or physics mods.

## Report a problem

Use the issue/reporting channel linked on the release page. Include:

- Builder's Balloon, Minecraft, NeoForge and relevant optional-mod versions.
- Whether it occurred in singleplayer or multiplayer; client and server logs where relevant.
- Reproduction steps, expected behaviour and what happened.
- Whether the tether was wooden/metal, placed on a moving Sable sublevel, and active for a player.
- Resource packs/shaders involved and a screenshot or short video for visual issues.

Remove personal information from shared logs. If practical, check whether the problem occurs without the affected animation pack or in a minimal test instance.
