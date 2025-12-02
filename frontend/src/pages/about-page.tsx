"use client"

import type React from "react"
import { motion } from "framer-motion"
import { Sparkles, Users, Target, Heart, ArrowRight } from "lucide-react"
import Navbar from "@/components/layout/navbar"
import Footer from "@/components/layout/footer"
import PageContainer from "@/components/layout/page-container"
import { Button } from "@/components/ui/button"
import { Link } from "react-router"

const values = [
  {
    icon: Users,
    title: "Community First",
    description: "We believe in bringing people together through unforgettable experiences.",
  },
  {
    icon: Target,
    title: "Simplicity",
    description: "Making event management accessible and straightforward for everyone.",
  },
  {
    icon: Heart,
    title: "Passion",
    description: "We're passionate about helping organizers create amazing events.",
  },
]

const team = [
  { name: "Event Organizers", count: "1,000+", description: "Trust our platform" },
  { name: "Events Hosted", count: "10,000+", description: "Successfully completed" },
  { name: "Tickets Sold", count: "500K+", description: "Happy attendees" },
]

const AboutPage: React.FC = () => {
  return (
    <PageContainer>
      <Navbar />

      {/* Hero Section */}
      <section className="relative pt-24 pb-16 overflow-hidden">
        <div className="absolute inset-0 gradient-mesh opacity-50" />
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-primary/20 rounded-full blur-3xl" />

        <div className="container mx-auto px-4 lg:px-8 relative">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            className="text-center max-w-3xl mx-auto"
          >
            <div className="inline-flex items-center gap-2 glass px-4 py-2 rounded-full mb-6">
              <Sparkles className="w-4 h-4 text-primary" />
              <span className="text-sm text-foreground">About EventHub</span>
            </div>

            <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold text-foreground mb-6">
              Connecting People Through{" "}
              <span className="text-gradient">Amazing Events</span>
            </h1>

            <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
              EventHub is your complete event management platform. We help organizers create,
              manage, and sell tickets for events of all sizes, while providing attendees with
              seamless discovery and booking experiences.
            </p>
          </motion.div>
        </div>
      </section>

      {/* Mission Section */}
      <section className="py-16">
        <div className="container mx-auto px-4 lg:px-8">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            <motion.div
              initial={{ opacity: 0, x: -30 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
            >
              <h2 className="text-3xl font-bold text-foreground mb-6">Our Mission</h2>
              <p className="text-muted-foreground mb-6 leading-relaxed">
                We're on a mission to democratize event management. Whether you're hosting a small
                community meetup or a large-scale conference, EventHub provides the tools you need
                to succeed.
              </p>
              <p className="text-muted-foreground leading-relaxed">
                Our platform combines powerful features with an intuitive interface, making it easy
                for anyone to create professional events. From ticket sales to QR code validation,
                we've got you covered.
              </p>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, x: 30 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              className="glass rounded-2xl p-8"
            >
              <div className="grid grid-cols-3 gap-6">
                {team.map((stat, index) => (
                  <div key={index} className="text-center">
                    <p className="text-3xl font-bold text-gradient mb-1">{stat.count}</p>
                    <p className="text-sm text-foreground font-medium">{stat.name}</p>
                    <p className="text-xs text-muted-foreground">{stat.description}</p>
                  </div>
                ))}
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      {/* Values Section */}
      <section className="py-16">
        <div className="container mx-auto px-4 lg:px-8">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-12"
          >
            <h2 className="text-3xl font-bold text-foreground mb-4">Our Values</h2>
            <p className="text-muted-foreground max-w-xl mx-auto">
              These principles guide everything we do at EventHub.
            </p>
          </motion.div>

          <div className="grid md:grid-cols-3 gap-8">
            {values.map((value, index) => {
              const Icon = value.icon
              return (
                <motion.div
                  key={value.title}
                  initial={{ opacity: 0, y: 20 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: index * 0.1 }}
                  className="glass rounded-2xl p-6 text-center card-hover"
                >
                  <div className="w-14 h-14 rounded-2xl gradient-primary flex items-center justify-center mx-auto mb-4">
                    <Icon className="w-7 h-7 text-white" />
                  </div>
                  <h3 className="text-xl font-semibold text-foreground mb-2">{value.title}</h3>
                  <p className="text-muted-foreground">{value.description}</p>
                </motion.div>
              )
            })}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-16">
        <div className="container mx-auto px-4 lg:px-8">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true }}
            className="relative overflow-hidden rounded-3xl gradient-primary p-8 md:p-12 text-center"
          >
            <div className="relative">
              <h2 className="text-3xl md:text-4xl font-bold text-white mb-4">
                Ready to Get Started?
              </h2>
              <p className="text-white/80 max-w-xl mx-auto mb-8">
                Join thousands of organizers who trust EventHub for their events.
              </p>
              <div className="flex flex-wrap justify-center gap-4">
                <Link to="/organizers">
                  <Button size="lg" className="bg-white text-primary hover:bg-white/90 font-semibold">
                    Start Organizing
                    <ArrowRight className="w-4 h-4 ml-2" />
                  </Button>
                </Link>
                <Link to="/">
                  <Button
                    size="lg"
                    variant="outline"
                    className="border-white/30 text-white hover:bg-white/10"
                  >
                    Browse Events
                  </Button>
                </Link>
              </div>
            </div>
          </motion.div>
        </div>
      </section>

      <Footer />
    </PageContainer>
  )
}

export default AboutPage
