package tz.elmkusoma.course.domain;

/**
 * Declared broadcast source of a live class — one unified source model for the
 * session (not one Live implementation per device). The actual media always
 * travels through the existing LiveKit/WebRTC room; this value records which
 * physical/production source the teacher broadcasts from so the control room,
 * learners and operators know what to expect.
 *
 * BROWSER                laptop/desktop browser (built-in or default camera)
 * MOBILE                 phone/tablet browser (front or rear camera)
 * USB_CAMERA             external USB camera selected in the browser
 * PROFESSIONAL_CAMERA    DSLR/mirrorless/PTZ via capture card or encoder
 * OBS                    OBS Studio pushed over WHIP/RTMP/SRT ingest
 * ENCODER                hardware encoder pushed over supported ingest
 * STUDIO                 studio production system pushed over supported ingest
 * OTHER                  any other supported source
 */
public enum LiveBroadcastSource {
    BROWSER,
    MOBILE,
    USB_CAMERA,
    PROFESSIONAL_CAMERA,
    OBS,
    ENCODER,
    STUDIO,
    OTHER
}
