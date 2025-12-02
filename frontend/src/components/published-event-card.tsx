import { PublishedEventSummary } from "@/domain/domain";
import { Card } from "./ui/card";
import { Calendar, Heart, MapPin, Share2 } from "lucide-react";
import { format } from "date-fns";
import { Link } from "react-router";
import RandomEventImage from "./random-event-image";
import { useState } from "react";
import toast from "react-hot-toast";

interface PublishedEventCardProperties {
  publishedEvent: PublishedEventSummary;
}

const PublishedEventCard: React.FC<PublishedEventCardProperties> = ({
  publishedEvent,
}) => {
  const [isLiked, setIsLiked] = useState(false);

  const handleLike = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsLiked(!isLiked);
  };

  const handleShare = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    
    const eventUrl = `${window.location.origin}/events/${publishedEvent.id}`;
    const shareData = {
      title: publishedEvent.name,
      text: `Check out this event: ${publishedEvent.name}`,
      url: eventUrl,
    };

    try {
      if (navigator.share && navigator.canShare?.(shareData)) {
        await navigator.share(shareData);
        toast.success("Shared successfully!");
      } else {
        await navigator.clipboard.writeText(eventUrl);
        toast.success("Link copied to clipboard!");
      }
    } catch (error) {
      if ((error as Error).name !== "AbortError") {
        try {
          await navigator.clipboard.writeText(eventUrl);
          toast.success("Link copied to clipboard!");
        } catch {
          toast.error("Failed to share event");
        }
      }
    }
  };

  return (
    <Link to={`/events/${publishedEvent.id}`}>
      <Card className="py-0 overflow-hidden max-w-[240px] gap-2">
        {/* Card Image */}
        <div className="h-[140px]">
          <RandomEventImage />
        </div>
        <div className="px-2">
          <h3 className="text-lg font-medium">{publishedEvent.name}</h3>
        </div>
        <div className="px-2">
          <div className="flex gap-2 text-sm mb-2 text-gray-500">
            <MapPin className="w-5" /> {publishedEvent.venue}
          </div>
          <div className="flex gap-2 text-sm mb-2 text-gray-500">
            {publishedEvent.start && publishedEvent.end ? (
              <div className="flex gap-2">
                <Calendar className="w-5" />{" "}
                {format(publishedEvent.start, "PP")} -{" "}
                {format(publishedEvent.end, "PP")}
              </div>
            ) : (
              <div className="flex gap-2">
                <Calendar />
                Dates TBD
              </div>
            )}
          </div>
          <div className="flex justify-between p-2 border-t text-gray-500">
            <button 
              className={`cursor-pointer transition-colors ${isLiked ? "text-red-500" : "hover:text-red-400"}`}
              onClick={handleLike}
            >
              <Heart className={isLiked ? "fill-current" : ""} />
            </button>
            <button 
              className="cursor-pointer hover:text-primary transition-colors"
              onClick={handleShare}
            >
              <Share2 />
            </button>
          </div>
        </div>
      </Card>
    </Link>
  );
};

export default PublishedEventCard;
