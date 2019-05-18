package com.muzima.messaging.mms;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.annimon.stream.Stream;
import com.muzima.messaging.attachments.Attachment;
import com.muzima.utils.MediaUtil;

import org.whispersystems.libsignal.util.guava.Optional;

import java.util.LinkedList;
import java.util.List;

public class SlideDeck {
    private final List<Slide> slides = new LinkedList<>();

    public SlideDeck(@NonNull Context context, @NonNull List<? extends Attachment> attachments) {
        for (Attachment attachment : attachments) {
            Slide slide = MediaUtil.getSlideForAttachment(context, attachment);
            if (slide != null) slides.add(slide);
        }
    }

    public SlideDeck(@NonNull Context context, @NonNull Attachment attachment) {
        Slide slide = MediaUtil.getSlideForAttachment(context, attachment);
        if (slide != null) slides.add(slide);
    }

    public SlideDeck() {
    }

    public void clear() {
        slides.clear();
    }

    @NonNull
    public String getBody() {
        String body = "";

        for (Slide slide : slides) {
            Optional<String> slideBody = slide.getBody();

            if (slideBody.isPresent()) {
                body = slideBody.get();
            }
        }

        return body;
    }

    @NonNull
    public List<Attachment> asAttachments() {
        List<Attachment> attachments = new LinkedList<>();

        for (Slide slide : slides) {
            attachments.add(slide.asAttachment());
        }

        return attachments;
    }

    public void addSlide(Slide slide) {
        slides.add(slide);
    }

    public List<Slide> getSlides() {
        return slides;
    }

    public boolean containsMediaSlide() {
        for (Slide slide : slides) {
            if (slide.hasImage() || slide.hasVideo() || slide.hasAudio() || slide.hasDocument()) {
                return true;
            }
        }
        return false;
    }

    public @Nullable Slide getThumbnailSlide() {
        for (Slide slide : slides) {
            if (slide.hasImage()) {
                return slide;
            }
        }

        return null;
    }

    public @NonNull List<Slide> getThumbnailSlides() {
        return Stream.of(slides).filter(Slide::hasImage).toList();
    }

    public @Nullable AudioSlide getAudioSlide() {
        for (Slide slide : slides) {
            if (slide.hasAudio()) {
                return (AudioSlide)slide;
            }
        }

        return null;
    }

    public @Nullable DocumentSlide getDocumentSlide() {
        for (Slide slide: slides) {
            if (slide.hasDocument()) {
                return (DocumentSlide)slide;
            }
        }

        return null;
    }
}
